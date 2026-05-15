package com.cmt.NexusAi.modules.thumb.service.imp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.NexusAi.modules.thumb.mapper.ThumbMapper;
import com.cmt.NexusAi.modules.thumb.model.dto.DoThumbRequestDTO;
import com.cmt.NexusAi.modules.thumb.model.entity.Thumb;
import com.cmt.NexusAi.modules.thumb.service.ThumbService;
import jakarta.servlet.http.HttpServletRequest;

public class ThumbServiceRedisImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {
    @Override
    public Boolean doThumb(DoThumbRequestDTO doThumbRequest, HttpServletRequest request) {
        return null;
    }

    @Override
    public Boolean undoThumb(DoThumbRequestDTO doThumbRequest, HttpServletRequest request) {
        return null;
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        return null;
    }
}
