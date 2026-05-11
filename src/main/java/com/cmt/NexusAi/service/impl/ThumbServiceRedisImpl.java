package com.cmt.NexusAi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.NexusAi.mapper.ThumbMapper;
import com.cmt.NexusAi.model.dto.thumb.DoThumbRequest;
import com.cmt.NexusAi.model.entity.Thumb;
import com.cmt.NexusAi.service.ThumbService;
import jakarta.servlet.http.HttpServletRequest;

public class ThumbServiceRedisImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {
    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        return null;
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        return null;
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        return null;
    }
}
