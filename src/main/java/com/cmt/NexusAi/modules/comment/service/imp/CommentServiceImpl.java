package com.cmt.NexusAi.modules.comment.service.imp;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.NexusAi.common.enums.ViolationTag;
import com.cmt.NexusAi.common.service.UserScoreService;
import com.cmt.NexusAi.modules.audit.L1.sensitive.common.HitResult;
import com.cmt.NexusAi.modules.audit.L2a.entity.SimHashResult;
import com.cmt.NexusAi.modules.audit.L2a.service.ShadowAuditService;
import com.cmt.NexusAi.modules.audit.L2a.service.SimHashCacheService;
import com.cmt.NexusAi.modules.audit.L2b.constant.AiContentAuditConstant;
import com.cmt.NexusAi.modules.audit.L1.sensitive.content.ContentAuditor;
import com.cmt.NexusAi.modules.comment.mapper.CommentMapper;
import com.cmt.NexusAi.modules.comment.mapper.CommentReportMapper;
import com.cmt.NexusAi.modules.comment.model.dto.CommentAddDTO;
import com.cmt.NexusAi.modules.comment.model.dto.ReportRequestDTO;
import com.cmt.NexusAi.modules.comment.model.entity.Comment;
import com.cmt.NexusAi.modules.comment.model.entity.CommentReport;
import com.cmt.NexusAi.modules.comment.model.vo.CommentVO;
import com.cmt.NexusAi.modules.comment.service.CommentService;
import com.cmt.NexusAi.modules.user.model.entity.User;
import com.cmt.NexusAi.modules.user.service.UserService;
import com.cmt.NexusAi.modules.security.util.SecurityUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private ContentAuditor contentAuditor;
    @Resource
    private UserService userService;
    @Resource
    private CommentReportMapper commentReportMapper;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private ShadowAuditService shadowAuditService;
    @Resource
    private SimHashCacheService simHashCacheService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserScoreService userScoreService;

    private static final Pattern CONTACT_PATTERN = Pattern.compile(
            "(?i)(微信|vx|weixin|加微|加v|薇|威|手机号|电话|qq|ＱＱ|链接|http|www|\\.com|\\.cn)"
    );
    private static final Pattern COMMERCIAL_PATTERN = Pattern.compile(
            "(?i)(代发|广告|兼职|推广|包过|刷单|返利|代购|引流|办证|贷款|代写|论文|发票|博彩|色情|" +
                    "限时抢购|全场.*折|秒杀|清仓|大促|特价|优惠|立减|包邮|速来|错过再等|一折|两折|三折|抢购|快来买|手慢无)"
    );

    @Override
    public CommentVO addComment(CommentAddDTO dto) {
        String content = dto.getContent();
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userService.getById(userId);
        LocalDateTime registerTime = user != null ? user.getRegisterTime() : null;
        boolean newUser = isNewUser(registerTime);

        // 禁言拦截
        if (userScoreService.isMuted(userId)) {
            throw new RuntimeException("您因违规被限制发言，请24小时后再试");
        }

        // ===== L1：AC自动机（只配P0/P1）+ 正则 =====
        List<HitResult> hits = contentAuditor.match(content);
        boolean p0Hit = hits != null && hits.stream().anyMatch(h -> "P0".equals(h.getRiskLevel()));
        boolean p1Hit = hits != null && hits.stream().anyMatch(h -> "P1".equals(h.getRiskLevel()));

        boolean hasContact = CONTACT_PATTERN.matcher(content).find();
        boolean hasCommercial = COMMERCIAL_PATTERN.matcher(content).find();
        // P1拦截条件：AC命中P1，且(联系方式+商业引流)或单独商业引流
        boolean p1Block = p1Hit && ((hasContact && hasCommercial) || hasCommercial);

        printL1Decision(hits, p0Hit, p1Hit, p1Block, hasContact, hasCommercial, content);

        // P0 或 P1正则命中 → 直接拒绝 + 加分
        if (p0Hit || p1Block) {
            // P1影子采样1%：对即将被P1拦截的内容，放行送AI验证是否误杀
            // P0不采样（P0是黑名单，确定性极高，无需验证）
            if (!p0Hit && p1Block && shadowAuditService.isL1P1ShadowSampled()) {
                Comment c = saveCommentEntity(dto, userId, "VISIBLE", "PENDING", "PASS");
                sendMqAsync(c.getId(), "L1_P1_SHADOW", "P1误杀抽检-放行验证", userId);
                log.info("[L1-影子] P1误杀抽检命中，放行送AI验证 | userId={}", userId);
                return toVO(c, "评论发布成功");
            }

            boolean isP0 = p0Hit;
            String reason = isP0 ? "P0-AC自动机拦截" : "P1-引流话术拦截";
            String tag = isP0 ? ViolationTag.P0_BLACKLIST.getTag() : ViolationTag.P1_CONTACT.getTag();
            ViolationTag scoreTag = isP0 ? ViolationTag.P0_BLACKLIST : ViolationTag.P1_CONTACT;

            Comment c = saveCommentEntity(dto, userId, "HIDDEN", "REJECTED", tag);
            userScoreService.addScore(userId, scoreTag, c.getId().toString());
            return toVO(c, reason);
        }

        // P1命中但正则未命中 → 不拦截，直接放行到SimHash（无需影子采样，因为未被拦截）

        // ===== L2a：SimHash缓存 =====
        SimHashResult cached = simHashCacheService.query(content);

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║         [决策-L2a] SimHash缓存查询         ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║ 文本: " + (content.length() > 40 ? content.substring(0, 40) + "..." : content));
        System.out.println("║ 缓存命中: " + (cached != null ? "⚠️ 是（复用历史审核结果）" : "✅ 否，进入下一步"));
        System.out.println("╚══════════════════════════════════════════╝");

        if (cached != null) {
            String tag = cached.getViolationTag();

            // SimHash影子采样1%：全量用户，验证缓存是否污染（误杀）
            // 新老用户都可能命中历史违规内容指纹，均需抽检
            if (shadowAuditService.isSimHashShadowSampled()) {
                log.info("[SimHash-影子] 缓存命中但抽检送AI复核 | userId={} | newUser={}", userId, newUser);
                Comment c = saveCommentEntity(dto, userId, "VISIBLE", "PENDING", "PASS");
                sendMqAsync(c.getId(), "SIMHASH_SHADOW", "SimHash缓存污染抽检", userId);
                return toVO(c, "评论发布成功");
            }

            // 99%命中直接拒绝
            Comment c = saveCommentEntity(dto, userId, "HIDDEN", "REJECTED", tag);
            userScoreService.addScore(userId, ViolationTag.valueOf(tag), c.getId().toString());
            return toVO(c, "SimHash复用历史审核结果");
        }

        // ===== L1.5：行为兜底（纯行为，无正则语义判断） =====
        int userRisk = getUserRiskLevel(userId, user);
        int contactFreq = 0;
        if (hasContact) {
            String contactKey = "user:contact:" + userId;
            Long cnt = stringRedisTemplate.opsForValue().increment(contactKey);
            if (cnt != null && cnt == 1) {
                stringRedisTemplate.expire(contactKey, Duration.ofHours(24));
            }
            contactFreq = cnt != null ? cnt.intValue() : 0;
            if (contactFreq >= 3) {
                log.warn("[L1.5-引流] userId={} 24h内留联系方式{}次", userId, contactFreq);
            }
        }

        boolean forceAudit = (userRisk >= 3)
                || (userRisk >= 2 && contactFreq >= 2)
                || (contactFreq >= 3);

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║        [决策-L1.5] 风险画像+行为兜底         ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║ 用户风险: " + userRisk);
        System.out.println("║ 联系方式频次(24h): " + contactFreq);
        System.out.println("║ 触发规则: " + (forceAudit ? "强制送AI终审" : "未触发强制审核"));
        System.out.println("║ 业务动作: " + (forceAudit ? "强制送AI终审" : "进入统一路由分流"));
        System.out.println("╚══════════════════════════════════════════╝");

        if (forceAudit) {
            String reason = userRisk >= 3 ? "高风险/新用户行为兜底"
                    : "联系方式频次兜底(24h≥" + contactFreq + "次)";
            Comment c = saveCommentEntity(dto, userId, "VISIBLE", "PENDING", "PASS");
            sendMqAsync(c.getId(), "L15_BEHAVIOR", reason, userId);
            return toVO(c, "评论发布成功");
        }

        // ===== 统一路由分流 =====
        boolean isSample = !newUser && shadowAuditService.isSampled(); // 5%漏杀抽检

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║           [决策-路由] 用户分流决策          ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║ 新用户(7天内): " + newUser);
        System.out.println("║ 影子采样(5%):  " + isSample);
        System.out.println("║ 路由结论: " + (newUser || isSample ? "送MQ异步AI审核" : "直接放行"));
        System.out.println("╚══════════════════════════════════════════╝");

        if (newUser || isSample) {
            String reason = newUser ? "新用户强制AI终审" : "影子采样命中";
            String source = newUser ? "NEW_USER" : "SHADOW_SAMPLE";
            Comment c = saveCommentEntity(dto, userId, "VISIBLE", "PENDING", "PASS");
            sendMqAsync(c.getId(), source, reason, userId);
            return toVO(c, "评论发布成功");
        }

        // 放行出口（全量异步再审兜底）
        Comment c = saveCommentEntity(dto, userId, "VISIBLE", "PASSED", "PASS");
        sendMqAsync(c.getId(), "ASYNC_AUDIT", "全量异步再审", userId);
        return toVO(c, "评论发布成功");
    }

    private void printL1Decision(List<HitResult> hits, boolean p0Hit, boolean p1Hit, boolean p1Block,
    boolean hasContact, boolean hasCommercial, String content) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║        [决策-L1] AC自动机+正则决策          ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║ 文本: " + (content.length() > 40 ? content.substring(0, 40) + "..." : content));
        System.out.println("║ 命中词数: " + (hits == null ? 0 : hits.size()));

        if (hits != null && !hits.isEmpty()) {
            System.out.println("║ 命中详情:");
            for (HitResult hit : hits) {
                System.out.println("║   - [" + hit.getRiskLevel() + "] " + hit.getMatchedWord());
            }
            System.out.println("║ 最高风险等级: " + hits.stream()
                    .map(HitResult::getRiskLevel)
                    .max(String::compareTo)
                    .orElse("无"));
        } else {
            System.out.println("║ 命中详情: 无");
        }

        System.out.println("║ 正则-联系方式: " + hasContact);
        System.out.println("║ 正则-商业引流: " + hasCommercial);
        System.out.println("║ P0命中(直接拦截): " + p0Hit);
        System.out.println("║ P1命中: " + p1Hit);
        System.out.println("║ P1+正则命中(拦截): " + p1Block);
        System.out.println("╚══════════════════════════════════════════╝");
    }

    private int getUserRiskLevel(Long userId, User user) {
        if (user == null || user.getRegisterTime() == null) return 3;
        if (isNewUser(user.getRegisterTime())) return 3;

        long total = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, userId)
        );
        long rejected = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, userId)
                        .eq(Comment::getAuditResult, "REJECTED")
        );
        if (total > 0 && rejected * 100 / total > 20) return 2;

        long reportCount = commentReportMapper.selectCount(
                new LambdaQueryWrapper<CommentReport>()
                        .inSql(CommentReport::getCommentId,
                                "SELECT id FROM comment WHERE user_id = " + userId)
        );
        if (reportCount > 3) return 2;

        Date oneHourAgo = new Date(System.currentTimeMillis() - 3600_000);
        long recentCount = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, userId)
                        .ge(Comment::getCreateTime, oneHourAgo)
        );
        if (recentCount > 10) return 2;

        if (user.getRegisterTime().isBefore(LocalDateTime.now().minusDays(30))
                && total > 10 && rejected == 0) {
            return 0;
        }
        return 1;
    }

    private boolean isNewUser(LocalDateTime registerTime) {
        return registerTime != null &&
                registerTime.isAfter(LocalDateTime.now().minusDays(7));
    }

    private Comment saveCommentEntity(CommentAddDTO dto, Long userId,
            String displayStatus, String auditResult, String violationTag) {
        Comment comment = new Comment();
        BeanUtil.copyProperties(dto, comment);
        comment.setUserId(userId);
        comment.setDisplayStatus(displayStatus);
        comment.setAuditResult(auditResult);
        comment.setViolationTag(violationTag);
        comment.setCreateTime(new Date());
        this.save(comment);
        return comment;
    }

    private CommentVO toVO(Comment comment, String auditDesc) {
        CommentVO vo = BeanUtil.copyProperties(comment, CommentVO.class);
        vo.setAuditDesc(auditDesc);
        return vo;
    }

    private void sendMqAsync(Long commentId, String source, String reason, Long userId) {
        String payload = String.format(
                "{\"commentId\":%d,\"source\":\"%s\",\"reason\":\"%s\",\"userId\":%d}",
                commentId, source, reason, userId
        );
        rocketMQTemplate.asyncSend(AiContentAuditConstant.AUDIT_TOPIC, payload, new SendCallback() {
            @Override public void onSuccess(SendResult sendResult) {
                log.info("[决策-MQ] 审核消息发送成功 | commentId={} | source={}", commentId, source);
            }
            @Override public void onException(Throwable e) {
                log.error("[决策-MQ] 审核消息发送失败 | commentId={}", commentId, e);
            }
        });
    }

    @Override
    public List<CommentVO> getCommentsByBlogId(Long blogId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getBlogId, blogId)
                .eq(Comment::getDisplayStatus, "VISIBLE")
                .orderByDesc(Comment::getCreateTime);
        List<Comment> list = this.list(wrapper);
        return list.stream()
                .map(c -> BeanUtil.copyProperties(c, CommentVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void reportComment(Long commentId, ReportRequestDTO request) {
        Long userId = SecurityUtil.getCurrentUserId();
        String lockKey = String.format("report:lock:%d:%d", userId, commentId);
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofHours(24));
        if (Boolean.FALSE.equals(locked)) {
            log.warn("[举报] userId={} 重复举报 commentId={}", userId, commentId);
            return;
        }
        CommentReport report = new CommentReport();
        report.setCommentId(commentId);
        report.setReporterId(userId);
        report.setReportReason(request.getReason());
        report.setReportDesc(request.getDesc());
        report.setStatus(0);
        commentReportMapper.insert(report);
        checkReportThreshold(commentId);
        log.info("[举报] userId={} 举报 commentId={}，原因={}", userId, commentId, request.getReason());
    }

    private void checkReportThreshold(Long commentId) {
        String countKey = String.format("report:count:%d", commentId);
        Long count = stringRedisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(countKey, Duration.ofDays(7));
        }
        if (count != null && count >= 3) {
            Comment comment = this.getById(commentId);
            if (comment == null || "REJECTED".equals(comment.getAuditResult())) {
                return;
            }
            lambdaUpdate()
                    .eq(Comment::getId, commentId)
                    .set(Comment::getDisplayStatus, "HIDDEN")
                    .set(Comment::getAuditResult, "REJECTED")
                    .set(Comment::getViolationTag, ViolationTag.REPORT_HIT.getTag())
                    .update();

            userScoreService.addScore(comment.getUserId(), ViolationTag.REPORT_HIT, commentId.toString());

            String payload = String.format(
                    "{\"commentId\":%d,\"source\":\"%s\",\"reason\":\"%s\",\"userId\":%d}",
                    comment.getId(), "REPORT_AUDIT", "举报达阈值", comment.getUserId()
            );
            rocketMQTemplate.asyncSend(AiContentAuditConstant.AUDIT_TOPIC, payload, new SendCallback() {
                @Override public void onSuccess(SendResult sendResult) {
                    log.info("[决策-MQ] 举报复核消息发送成功 | commentId={}", comment.getId());
                }
                @Override public void onException(Throwable e) {
                    log.error("[决策-MQ] 举报复核消息发送失败 | commentId={}", comment.getId(), e);
                }
            });
            log.warn("[举报兜底] commentId={} 累计举报{}次，已隐藏并送AI复核", commentId, count);
        }
    }
}