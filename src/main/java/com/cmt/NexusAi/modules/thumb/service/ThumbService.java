package com.cmt.NexusAi.modules.thumb.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.cmt.NexusAi.modules.thumb.model.dto.DoThumbRequestDTO;
import com.cmt.NexusAi.modules.thumb.model.entity.Thumb;
import jakarta.servlet.http.HttpServletRequest;

public interface ThumbService extends IService<Thumb> {

    /**
     * 点赞
     * @param doThumbRequest
     * @param request
     * @return {@link Boolean }
     */
    Boolean doThumb(DoThumbRequestDTO doThumbRequest, HttpServletRequest request);

    /**
     * 取消点赞
     * @param doThumbRequest
     * @param request
     * @return {@link Boolean }
     */
    Boolean undoThumb(DoThumbRequestDTO doThumbRequest, HttpServletRequest request);


    /**
     * 是否已经点赞
     * @param blogId 博客id
     * @param userId 用户id
     * @return
     */
    Boolean hasThumb(Long blogId, Long userId);

}
