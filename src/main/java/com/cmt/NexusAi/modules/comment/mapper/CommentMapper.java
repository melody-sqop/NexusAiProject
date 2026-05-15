package com.cmt.NexusAi.modules.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmt.NexusAi.modules.comment.model.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}