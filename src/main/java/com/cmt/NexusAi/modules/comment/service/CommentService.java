package com.cmt.NexusAi.modules.comment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmt.NexusAi.modules.comment.model.dto.CommentAddDTO;
import com.cmt.NexusAi.modules.comment.model.dto.ReportRequestDTO;
import com.cmt.NexusAi.modules.comment.model.entity.Comment;
import com.cmt.NexusAi.modules.comment.model.vo.CommentVO;

import java.util.List;

public interface CommentService extends IService<Comment> {
    /**
     * 发表评论
     */
    CommentVO addComment(CommentAddDTO dto);

    /**
     * 查询某博客的评论列表（仅展示审核通过的）
     */
    List<CommentVO> getCommentsByBlogId(Long blogId);


    // 新增：用户举报
    void reportComment(Long commentId, ReportRequestDTO request);
}
