package com.cmt.yutumblike.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.yutumblike.mapper.ThumbMapper;
import com.cmt.yutumblike.model.dto.thumb.DoThumbRequest;
import com.cmt.yutumblike.model.entity.Thumb;
import com.cmt.yutumblike.service.ThumbService;
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
