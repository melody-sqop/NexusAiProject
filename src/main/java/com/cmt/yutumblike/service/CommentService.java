package com.cmt.yutumblike.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmt.yutumblike.model.dto.CommentAddDTO;
import com.cmt.yutumblike.model.entity.Comment;
import com.cmt.yutumblike.model.vo.CommentVO;

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
}
