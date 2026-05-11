package com.cmt.NexusAi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.NexusAi.ai.constant.AiContentAuditConstant;
import com.cmt.NexusAi.mapper.CommentMapper;
import com.cmt.NexusAi.model.dto.CommentAddDTO;
import com.cmt.NexusAi.model.entity.Comment;
import com.cmt.NexusAi.model.vo.CommentVO;
import com.cmt.NexusAi.service.CommentService;
import com.cmt.NexusAi.util.SecurityUtil;
import jakarta.annotation.Resource;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

     @Resource
    private PulsarTemplate<String> pulsarTemplate;

    /**
     * 创建评论 并将评论内容纳入ai审核
     * @param dto 前端传来的评论实体类
     * @return
     */
    @Override
    public CommentVO addComment(CommentAddDTO dto) {
        // 1. 组装实体
        Comment comment = new Comment();
        BeanUtil.copyProperties(dto, comment);


        // 2. 设置当前用户
        comment.setUserId(SecurityUtil.getCurrentUserId());

        // 3. 设置初始状态：待审核
        comment.setAuditStatus(AiContentAuditConstant.PENDING);

        // 避免空指针 直接放入当前时间
        comment.setCreateTime(new Date());
        // 4. 存入数据库
        this.save(comment);

        // 5. TODO: 发送异步消息到 MQ，不阻塞主线程
        pulsarTemplate.sendAsync(AiContentAuditConstant.AUDIT_TOPIC, String.valueOf(comment.getId()));

        // 6. 组装返回 VO
        CommentVO commentVO = BeanUtil.copyProperties(comment, CommentVO.class);
        commentVO.setAuditDesc("评论已提交，正在审核中");

        return commentVO;
    }

    @Override
    public List<CommentVO> getCommentsByBlogId(Long blogId) {
        // 1. 查询条件：只查该博客下 审核通过 的评论
        // 注意：如果是查看“自己的评论”，可以放宽条件让用户看到自己待审核的状态
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getBlogId, blogId)
                .eq(Comment::getAuditStatus, AiContentAuditConstant.PASSED)
                .orderByDesc(Comment::getCreateTime);

        List<Comment> list = this.list(wrapper);

        // 2. 转 VO (这里可以根据需要关联 User 表查昵称头像)
        return list.stream()
                .map(comment -> BeanUtil.copyProperties(comment, CommentVO.class))
                .collect(Collectors.toList());
    }

}