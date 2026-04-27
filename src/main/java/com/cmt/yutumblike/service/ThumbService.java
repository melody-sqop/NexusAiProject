package com.cmt.yutumblike.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.cmt.yutumblike.model.dto.thumb.DoThumbRequest;
import com.cmt.yutumblike.model.entity.Thumb;
import jakarta.servlet.http.HttpServletRequest;

public interface ThumbService extends IService<Thumb> {

    /**
     * 点赞
     * @param doThumbRequest
     * @param request
     * @return {@link Boolean }
     */
    Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);

    /**
     * 取消点赞
     * @param doThumbRequest
     * @param request
     * @return {@link Boolean }
     */
    Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);


    /**
     * 是否已经点赞
     * @param blogId 博客id
     * @param userId 用户id
     * @return
     */
    Boolean hasThumb(Long blogId, Long userId);

}
