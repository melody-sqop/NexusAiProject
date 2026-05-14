package com.cmt.NexusAi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.NexusAi.constant.RedisLuaScriptConstant;
import com.cmt.NexusAi.listener.thumb.msg.ThumbEvent;
import com.cmt.NexusAi.mapper.ThumbMapper;
import com.cmt.NexusAi.model.dto.thumb.DoThumbRequest;
import com.cmt.NexusAi.model.entity.Thumb;
import com.cmt.NexusAi.model.entity.User;
import com.cmt.NexusAi.model.enums.LuaStatusEnum;
import com.cmt.NexusAi.service.ThumbService;
import com.cmt.NexusAi.service.UserService;
import com.cmt.NexusAi.util.RedisKeyUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// 消息队列异步优化
@Service("thumbService")
@Slf4j
@RequiredArgsConstructor
public class ThumbServiceMQImpl extends ServiceImpl<ThumbMapper, Thumb>
        implements ThumbService {


    private final RedisTemplate<String, Object> redisTemplate;

    private final RocketMQTemplate rocketMQTemplate;



    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        Long loginUserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long blogId = doThumbRequest.getBlogId();
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUserId);
        // 执行 Lua 脚本，点赞存入 Redis
        long result = redisTemplate.execute(
                RedisLuaScriptConstant.THUMB_SCRIPT_MQ,
                List.of(userThumbKey),
                blogId
        );
        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new RuntimeException("用户已点赞");
        }

        ThumbEvent thumbEvent = ThumbEvent.builder()
                .blogId(blogId)
                .userId(loginUserId)
                .type(ThumbEvent.EventType.INCR)
                .eventTime(LocalDateTime.now())
                .build();

        // 发送点赞事件 不管有没有发送成功直接返回成功  兜底机制：回滚redis点赞记录
        rocketMQTemplate.asyncSend("thumb-topic", thumbEvent, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("点赞事件发送成功");
            }
            @Override
            public void onException(Throwable e) {
                redisTemplate.opsForHash().delete(userThumbKey, blogId.toString());
                log.error("点赞事件发送失败", e);
            }
        });

        return true;
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        Long loginUserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long blogId = doThumbRequest.getBlogId();
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUserId);
        // 执行 Lua 脚本，点赞记录从 Redis 删除
        long result = redisTemplate.execute(
                RedisLuaScriptConstant.UNTHUMB_SCRIPT_MQ,
                List.of(userThumbKey),
                blogId
        );
        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new RuntimeException("用户未点赞");
        }
        ThumbEvent thumbEvent = ThumbEvent.builder()
                .blogId(blogId)
                .userId(loginUserId)
                .type(ThumbEvent.EventType.DECR)
                .eventTime(LocalDateTime.now())
                .build();

        // 发送点赞事件 不管有没有发送成功直接返回成功  兜底机制：回滚redis点赞记录
        rocketMQTemplate.asyncSend("thumb-topic",
                MessageBuilder.withPayload(thumbEvent).build(),
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("取消点赞事件发送成功");
                    }
                    @Override
                    public void onException(Throwable e) {
                        redisTemplate.opsForHash().put(userThumbKey, blogId.toString(), true);
                        log.error("取消点赞事件发送失败: userId={}, blogId={}", loginUserId, blogId, e);
                    }
                }
        );

        return true;
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        return redisTemplate.opsForHash().hasKey(RedisKeyUtil.getUserThumbKey(userId), blogId.toString());
    }

}
