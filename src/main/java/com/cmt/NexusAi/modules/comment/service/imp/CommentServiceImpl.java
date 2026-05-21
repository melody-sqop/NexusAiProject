package com.cmt.NexusAi.modules.comment.service.imp;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.NexusAi.common.enums.ViolationTag;
import com.cmt.NexusAi.common.service.UserScoreService;
import com.cmt.NexusAi.modules.audit.L1.sensitive.common.HitResult;
import com.cmt.NexusAi.modules.audit.L1.sensitive.content.ContentAuditor;
import com.cmt.NexusAi.modules.audit.L2a.entity.SimHashResult;
import com.cmt.NexusAi.modules.audit.L2a.service.ShadowAuditService;
import com.cmt.NexusAi.modules.audit.L2a.service.SimHashCacheService;
import com.cmt.NexusAi.modules.audit.L2b.constant.AiContentAuditConstant;
import com.cmt.NexusAi.modules.audit.L2b.model.DTO.AuditContextDTO;
import com.cmt.NexusAi.modules.audit.L2b.model.DTO.AuditMessageDTO;
import com.cmt.NexusAi.modules.audit.L2b.model.vo.AuditResult;
import com.cmt.NexusAi.modules.audit.L2b.service.AuditRouteService;
import com.cmt.NexusAi.modules.audit.L2b.service.BlogTitleCacheService;
import com.cmt.NexusAi.modules.audit.L2b.service.SyncAiAuditService;
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
import com.fasterxml.jackson.databind.ObjectMapper;
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
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private AuditRouteService auditRouteService;
    @Resource
    private SyncAiAuditService syncAiAuditService;
    @Resource
    private BlogTitleCacheService blogTitleCacheService; // 1.3 新增

    private static final Pattern CONTACT_PATTERN = Pattern.compile("(?i)(微信|vx|weixin|加微|加v|薇|威|手机号|电话|qq|ＱＱ|链接|http|www|\\.com|\\.cn)");
    private static final Pattern COMMERCIAL_PATTERN = Pattern.compile("(?i)(代发|广告|兼职|推广|包过|刷单|返利|代购|引流|办证|贷款|代写|论文|发票|博彩|色情|限时抢购|全场.*折|秒杀|清仓|大促|特价|优惠|立减|包邮|速来|错过再等|一折|两折|三折|抢购|快来买|手慢无)");

    @Override
    public CommentVO addComment(CommentAddDTO dto) {
        String content = dto.getContent();
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userService.getById(userId);
        LocalDateTime registerTime = user != null ? user.getRegisterTime() : null;
        boolean newUser = isNewUser(registerTime);

        if (userScoreService.isMuted(userId)) {
            throw new RuntimeException("您因违规被限制发言，请24小时后再试");
        }

        // 1.3 核心：通过本地缓存获取父级标题快照
        String parentTitle = blogTitleCacheService.getTitle(dto.getBlogId());
        String scene = dto.getScene();

        List<HitResult> hits = contentAuditor.match(content);
        boolean p0Hit = hits != null && hits.stream().anyMatch(h -> "P0".equals(h.getRiskLevel()));
        boolean p1Hit = hits != null && hits.stream().anyMatch(h -> "P1".equals(h.getRiskLevel()));
        boolean hasContact = CONTACT_PATTERN.matcher(content).find();
        boolean hasCommercial = COMMERCIAL_PATTERN.matcher(content).find();
        boolean p1Block = p1Hit && ((hasContact && hasCommercial) || hasCommercial);

        if (p0Hit || p1Block) {
            if (!p0Hit && p1Block && shadowAuditService.isL1P1ShadowSampled()) {
                Comment c = saveCommentEntity(dto, userId, "VISIBLE", "PENDING", "PASS", parentTitle);
                sendMqAsync(c.getId(), "L1_P1_SHADOW", "P1误杀抽检-放行验证", userId, true, p1Hit, scene, parentTitle);
                return toVO(c, "评论发布成功");
            }
            boolean isP0 = p0Hit;
            String reason = isP0 ? "P0-AC自动机拦截" : "P1-引流话术拦截";
            ViolationTag scoreTag = isP0 ? ViolationTag.P0_BLACKLIST : ViolationTag.P1_CONTACT;
            Comment c = saveCommentEntity(dto, userId, "HIDDEN", "REJECTED", scoreTag.getTag(), parentTitle);
            userScoreService.addScore(userId, scoreTag, c.getId().toString());
            return toVO(c, reason);
        }

        SimHashResult cached = simHashCacheService.query(content);
        if (cached != null) {
            String tag = cached.getViolationTag();
            if (shadowAuditService.isSimHashShadowSampled()) {
                Comment c = saveCommentEntity(dto, userId, "VISIBLE", "PENDING", "PASS", parentTitle);
                sendMqAsync(c.getId(), "SIMHASH_SHADOW", "SimHash缓存污染抽检", userId, true, false, scene, parentTitle);
                return toVO(c, "评论发布成功");
            }
            Comment c = saveCommentEntity(dto, userId, "HIDDEN", "REJECTED", tag, parentTitle);
            userScoreService.addScore(userId, ViolationTag.safeFromTag(tag), c.getId().toString());
            return toVO(c, "SimHash复用历史审核结果");
        }

        int userRisk = getUserRiskLevel(userId, user);
        int contactFreq = 0;
        if (hasContact) {
            String contactKey = "user:contact:" + userId;
            Long cnt = stringRedisTemplate.opsForValue().increment(contactKey);
            if (cnt != null && cnt == 1) stringRedisTemplate.expire(contactKey, Duration.ofHours(24));
            contactFreq = cnt != null ? cnt.intValue() : 0;
        }
        boolean forceAudit = (userRisk >= 3) || (userRisk >= 2 && contactFreq >= 2) || (contactFreq >= 3);

        boolean isSample = !newUser && shadowAuditService.isSampled();
        AuditRouteService.RouteAction route = auditRouteService.resolveRoute(forceAudit, isSample);

        switch (route) {
            case SYNC_AI -> {
                return handleSyncAiRoute(dto, userId, content, p1Hit, scene, parentTitle);
            }
            case ASYNC_MQ -> {
                Comment c = saveCommentEntity(dto, userId, "VISIBLE", "PENDING", "PASS", parentTitle);
                sendMqAsync(c.getId(), "SHADOW_SAMPLE", "影子采样命中", userId, true, p1Hit, scene, parentTitle);
                return toVO(c, "评论发布成功");
            }
            case DIRECT_PASS -> {
                Comment c = saveCommentEntity(dto, userId, "VISIBLE", "PASSED", "PASS", parentTitle);
                sendMqAsync(c.getId(), "ASYNC_SAMPLE", "普通放行抽样", userId, false, p1Hit, scene, parentTitle);
                return toVO(c, "评论发布成功");
            }
        }
        throw new RuntimeException("路由决策异常");
    }

    // 1.3 修改：增加 scene 和 parentTitle 参数
    private CommentVO handleSyncAiRoute(CommentAddDTO dto, Long userId, String content, boolean p1Hit, String scene, String parentTitle) {
        AuditContextDTO context = AuditContextDTO.builder().content(content).scene(scene).parentTitle(parentTitle).build();
        AuditResult result = syncAiAuditService.auditWithTimeout(context);

        switch (result.action()) {
            case "block" -> {
                Comment c = saveCommentEntity(dto, userId, "SELF_ONLY", "REJECTED", ViolationTag.AI_REJECT.getTag(), parentTitle);
                userScoreService.addScore(userId, ViolationTag.AI_REJECT, c.getId().toString());
                simHashCacheService.cache(content, ViolationTag.AI_REJECT.getTag(), "SYNC_AI", "REJECT", result.reason());
                log.info("[同步AI] 判定违规 → SELF_ONLY影子封禁 | userId={}", userId);
                return toVO(c, "评论发布成功");
            }
            case "pass" -> {
                Comment c = saveCommentEntity(dto, userId, "VISIBLE", "PASSED", "PASS", parentTitle);
                log.info("[同步AI] 判定通过 → VISIBLE | userId={}", userId);
                return toVO(c, "评论发布成功");
            }
            default -> {
                Comment c = saveCommentEntity(dto, userId, "VISIBLE", "PENDING", "PASS", parentTitle);
                sendMqAsync(c.getId(), "SYNC_DEGRADE", "同步超时/需复核降级", userId, true, p1Hit, scene, parentTitle);
                log.info("[同步AI] 超时或需复核 → VISIBLE+PENDING转异步 | userId={}", userId);
                return toVO(c, "评论发布成功");
            }
        }
    }

    private int getUserRiskLevel(Long userId, User user) {
        if (user == null || user.getRegisterTime() == null) return 3;
        if (isNewUser(user.getRegisterTime())) return 3;
        String riskKey = "user:risk:" + userId;
        String riskStr = stringRedisTemplate.opsForValue().get(riskKey);
        if (riskStr != null) return Integer.parseInt(riskStr);
        long total = commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, userId));
        long rejected = commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, userId).eq(Comment::getAuditResult, "REJECTED"));
        List<Long> userCommentIds = commentMapper.selectList(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, userId).select(Comment::getId)).stream().map(Comment::getId).collect(Collectors.toList());
        long reportCount = userCommentIds.isEmpty() ? 0L : commentReportMapper.selectCount(new LambdaQueryWrapper<CommentReport>().in(CommentReport::getCommentId, userCommentIds));
        Date oneHourAgo = new Date(System.currentTimeMillis() - 3600_000);
        long recentCount = commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, userId).ge(Comment::getCreateTime, oneHourAgo));
        int riskLevel = 1;
        if (total > 0 && rejected * 100 / total > 20) riskLevel = 2;
        if (reportCount > 3) riskLevel = 2;
        if (recentCount > 10) riskLevel = 2;
        if (user.getRegisterTime().isBefore(LocalDateTime.now().minusDays(30)) && total > 10 && rejected == 0)
            riskLevel = 0;
        stringRedisTemplate.opsForValue().set(riskKey, String.valueOf(riskLevel), Duration.ofDays(1));
        return riskLevel;
    }

    private boolean isNewUser(LocalDateTime registerTime) {
        return registerTime != null && registerTime.isAfter(LocalDateTime.now().minusDays(7));
    }

    // 1.3 修改：增加 parentTitle 入库快照
    private Comment saveCommentEntity(CommentAddDTO dto, Long userId, String displayStatus, String auditResult, String violationTag, String parentTitle) {
        Comment comment = new Comment();
        BeanUtil.copyProperties(dto, comment);
        comment.setUserId(userId);
        comment.setDisplayStatus(displayStatus);
        comment.setAuditResult(auditResult);
        comment.setViolationTag(violationTag);
        comment.setParentTitle(parentTitle); // 快照冗余
        comment.setCreateTime(new Date());
        this.save(comment);
        return comment;
    }

    // 1.2 + 1.3 修改：影子封禁伪装 + 返回VO
    private CommentVO toVO(Comment comment, String auditDesc) {
        CommentVO vo = BeanUtil.copyProperties(comment, CommentVO.class);
        vo.setAuditDesc(auditDesc);
        // 影子封禁伪装：对前端永远展示为 VISIBLE，防止抓包识破
        if ("SELF_ONLY".equals(comment.getDisplayStatus())) {
            vo.setDisplayStatus("VISIBLE");
        }
        return vo;
    }

    // 1.3 修改：MQ发送增加 scene 和 parentTitle
    private void sendMqAsync(Long commentId, String source, String reason, Long userId, boolean needFullAudit, boolean l1P1Hit, String scene, String parentTitle) {
        try {
            AuditMessageDTO msg = AuditMessageDTO.builder()
                    .commentId(commentId)
                    .source(source)
                    .reason(reason)
                    .userId(userId)
                    .needFullAudit(needFullAudit)
                    .l1P1Hit(l1P1Hit)
                    .scene(scene)           // 上下文透传
                    .parentTitle(parentTitle)// 上下文透传
                    .build();
            String payload = objectMapper.writeValueAsString(msg);
            rocketMQTemplate.asyncSend(AiContentAuditConstant.AUDIT_TOPIC, payload, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("[决策-MQ] 审核消息发送成功 | commentId={} | source={}", commentId, source);
                }

                @Override
                public void onException(Throwable e) {
                    log.error("[决策-MQ] 审核消息发送失败 | commentId={}", commentId, e);
                }
            });
        } catch (Exception e) {
            log.error("[决策-MQ] 构造MQ消息异常 | commentId={}", commentId, e);
        }
    }

    // 1.2 修改：列表查询影子封禁对作者可见
    @Override
    public List<CommentVO> getCommentsByBlogId(Long blogId) {
        Long tempUserId = null;
        try {
            tempUserId = SecurityUtil.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户忽略
        }
        final Long currentUserId = tempUserId;

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getBlogId, blogId);

        if (currentUserId != null) {
            wrapper.and(w -> w
                    .eq(Comment::getDisplayStatus, "VISIBLE")
                    .or(sub -> sub
                            .eq(Comment::getDisplayStatus, "SELF_ONLY")
                            .eq(Comment::getUserId, currentUserId)
                    )
            );
        } else {
            wrapper.eq(Comment::getDisplayStatus, "VISIBLE");
        }

        wrapper.orderByDesc(Comment::getCreateTime);

        return this.list(wrapper).stream().map(c -> {
            CommentVO vo = BeanUtil.copyProperties(c, CommentVO.class);
            if ("SELF_ONLY".equals(c.getDisplayStatus())) {
                vo.setDisplayStatus("VISIBLE");
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void reportComment(Long commentId, ReportRequestDTO request) {
        Long userId = SecurityUtil.getCurrentUserId();
        String lockKey = String.format("report:lock:%d:%d", userId, commentId);
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofHours(24));
        if (Boolean.FALSE.equals(locked)) return;
        CommentReport report = new CommentReport();
        report.setCommentId(commentId);
        report.setReporterId(userId);
        report.setReportReason(request.getReason());
        report.setReportDesc(request.getDesc());
        report.setStatus(0);
        commentReportMapper.insert(report);
        checkReportThreshold(commentId);
    }

    private void checkReportThreshold(Long commentId) {
        String countKey = String.format("report:count:%d", commentId);
        Long count = stringRedisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1) stringRedisTemplate.expire(countKey, Duration.ofDays(7));
        if (count != null && count >= 3) {
            Comment comment = this.getById(commentId);
            if (comment == null || "REJECTED".equals(comment.getAuditResult())) return;
            lambdaUpdate().eq(Comment::getId, commentId).set(Comment::getDisplayStatus, "HIDDEN").set(Comment::getAuditResult, "REJECTED").set(Comment::getViolationTag, ViolationTag.REPORT_HIT.getTag()).update();
            userScoreService.addScore(comment.getUserId(), ViolationTag.REPORT_HIT, commentId.toString());
            // 举报达阈值时，拿不到 scene 和 parentTitle，传 null，消费端走老逻辑即可
            sendMqAsync(comment.getId(), "REPORT_AUDIT", "举报达阈值", comment.getUserId(), true, false, null, null);
        }
    }
}