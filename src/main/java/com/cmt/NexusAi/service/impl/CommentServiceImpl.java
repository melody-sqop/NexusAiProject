package com.cmt.NexusAi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.NexusAi.ai.common.HitResult;
import com.cmt.NexusAi.ai.constant.AiContentAuditConstant;
import com.cmt.NexusAi.ai.content.ContentAuditor;
import com.cmt.NexusAi.ai.model.enums.RiskLevel;
import com.cmt.NexusAi.ai.service.AiAuditService;
import com.cmt.NexusAi.mapper.CommentMapper;
import com.cmt.NexusAi.mapper.CommentReportMapper;
import com.cmt.NexusAi.mapper.UserMapper;
import com.cmt.NexusAi.model.dto.CommentAddDTO;
import com.cmt.NexusAi.model.dto.ReportRequestDTO;
import com.cmt.NexusAi.model.entity.Comment;
import com.cmt.NexusAi.model.entity.CommentReport;
import com.cmt.NexusAi.model.vo.CommentVO;
import com.cmt.NexusAi.service.CommentService;
import com.cmt.NexusAi.service.UserService;
import com.cmt.NexusAi.util.SecurityUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private ContentAuditor contentAuditor;  // ← 注入你的DFA匹配器

    @Resource
    private UserService userService;

    // 假设你已经有或即将创建 CommentReportMapper
    @Resource
    private CommentReportMapper commentReportMapper;

    @Resource
    private AiAuditService aiAuditService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public CommentVO addComment(CommentAddDTO dto) {
        // ========== 新增：L1 AC 预审 ==========
        List<HitResult> hits = contentAuditor.match(dto.getContent());
        RiskLevel.Result risk = RiskLevel.decide(hits, false);  // false=评论不是标题

        // 分支1：CRITICAL（涉政/暴恐）→ 直接拦截，不发MQ，不进AI
        if (risk.interceptNow()) {


            Comment comment = new Comment();
            BeanUtil.copyProperties(dto, comment);
            comment.setUserId(SecurityUtil.getCurrentUserId());
            comment.setAuditStatus(AiContentAuditConstant.REJECTED);  // 直接拒绝
            comment.setCreateTime(new Date());
            this.save(comment);  // 存库留痕

            log.warn("[L1拦截] commentId={} 命中敏感词，已拦截，action={}",
                    comment.getId(), risk.getAction());

            CommentVO vo = BeanUtil.copyProperties(comment, CommentVO.class);
            vo.setAuditDesc("内容包含违规信息，已被拦截");
            return vo;



        }


        boolean needL15 = userService.isNewUser(SecurityUtil.getCurrentUserId());

        if (needL15) {
            log.info("[L1.5兜底] 命中兜底策略，送AI复核 | userId={}",
                    SecurityUtil.getCurrentUserId());
        }

        // 分支2：需要 AI 复核（HIGH / MEDIUM且多命中）→ 走你原有的AI流程
        if (risk.needAi()) {


            Comment comment = new Comment();
            BeanUtil.copyProperties(dto, comment);
            comment.setUserId(SecurityUtil.getCurrentUserId());
            comment.setAuditStatus(AiContentAuditConstant.PENDING);
            comment.setCreateTime(new Date());
            this.save(comment);


            log.info("[L1送AI] commentId={} | level={} | action={} | hitWords={}",
                    comment.getId(),
                    risk.getLevel(),
                    risk.getAction(),
                    risk.getHits().stream().map(HitResult::getMatchedWord).collect(Collectors.toList()));



            // 原有 MQ 逻辑完全不变
            rocketMQTemplate.asyncSend(
                    AiContentAuditConstant.AUDIT_TOPIC,
                    String.valueOf(comment.getId()),
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("评论审核消息发送成功, commentId={}", comment.getId());
                        }
                        @Override
                        public void onException(Throwable e) {
                            log.error("评论审核消息发送失败, commentId={}", comment.getId(), e);
                        }
                    }
            );

            CommentVO vo = BeanUtil.copyProperties(comment, CommentVO.class);
            vo.setAuditDesc("评论已提交，正在审核中");
            return vo;
        }

        // 分支3：直接放行（LOW / MILD / MEDIUM）
        log.info("[COMMENT-PUBLISH] 【放行】L1直接通过，未触发AI ");

        // 分支3：LOW / SAFE / 单命中 MEDIUM → L1直接放行，不发MQ，不耗AI成本
        Comment comment = new Comment();
        BeanUtil.copyProperties(dto, comment);
        comment.setUserId(SecurityUtil.getCurrentUserId());
        comment.setAuditStatus(AiContentAuditConstant.PASSED);
        comment.setCreateTime(new Date());
        this.save(comment);

        log.info("[L1放行] commentId={} 直接通过", comment.getId());

        CommentVO vo = BeanUtil.copyProperties(comment, CommentVO.class);
        vo.setAuditDesc("评论已发布");
        return vo;
    }


    @Override
    public List<CommentVO> getCommentsByBlogId(Long blogId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getBlogId, blogId)
                .eq(Comment::getAuditStatus, AiContentAuditConstant.PASSED)
                .orderByDesc(Comment::getCreateTime);
        List<Comment> list = this.list(wrapper);
        return list.stream()
                .map(comment -> BeanUtil.copyProperties(comment, CommentVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void reportComment(Long commentId, ReportRequestDTO request) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 1. 防刷：同一用户对同一评论 24h 内只能举报 1 次
        String lockKey = String.format("report:lock:%d:%d", userId, commentId);
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofHours(24));
//        if (!Boolean.TRUE.equals(locked)) {
//            throw new RuntimeException("您已举报过该内容，24小时内请勿重复举报");
//        }

        // 2. 写入 MySQL 举报记录
        CommentReport report = new CommentReport();
        report.setCommentId(commentId);
        report.setReporterId(userId);
        report.setReportReason(request.getReason());
        report.setReportDesc(request.getDesc());
        report.setStatus(0);  // 待处理
        commentReportMapper.insert(report);

        // 3. 异步检查阈值
        CheckReportThreshold(commentId);

        log.info("[L1.5-举报] userId={} 举报 commentId={}，原因={}", userId, commentId, request.getReason());
    }

    private void CheckReportThreshold(Long commentId) {
        String countKey = String.format("report:count:%d", commentId);
        Long count = stringRedisTemplate.opsForValue().increment(countKey);
        stringRedisTemplate.expire(countKey, Duration.ofDays(7));

        if (count != null && count >= 3) {
            Comment comment = this.getById(commentId);
            if (comment == null || comment.getAuditStatus() == AiContentAuditConstant.REJECTED) {
                return;
            }

            // 达到阈值：隐藏 Comment 评论
            lambdaUpdate()
                    .eq(Comment::getId, commentId)
                    .set(Comment::getAuditStatus, AiContentAuditConstant.REJECTED)
                    .update();

            // 送 AI 复核（复用原有 topic）
            rocketMQTemplate.asyncSend(
                    AiContentAuditConstant.AUDIT_TOPIC,
                    String.valueOf(comment.getId()),
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("评论审核消息发送成功, commentId={}", comment.getId());
                        }
                        @Override
                        public void onException(Throwable e) {
                            log.error("评论审核消息发送失败, commentId={}", comment.getId(), e);
                        }
                    }
            );
            log.warn("[L1.5-举报兜底] commentId={} 累计举报{}次，已隐藏并送AI复核", commentId, count);
        }
    }



    private boolean isRandomSample() {
        return ThreadLocalRandom.current().nextDouble() < 0.05;  // 5% 概率
    }
}