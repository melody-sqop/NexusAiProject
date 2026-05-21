package com.cmt.NexusAi.modules.notice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmt.NexusAi.modules.notice.dto.NoticeQueryDTO;
import com.cmt.NexusAi.modules.notice.entity.Notice;

import java.util.List;

/**
 * 通知服务接口
 */
public interface NoticeService extends IService<Notice> {

    /**
     * 查询当前用户的通知列表（游标分页）
     * @param dto 查询参数
     * @return 通知列表
     */
    List<Notice> queryList(NoticeQueryDTO dto);

    /**
     * 将某条通知标记为已读
     * @param noticeId 通知ID
     */
    void readOne(Long noticeId);

    /**
     * 将当前用户的所有未读通知全部标记为已读
     */
    void readAll();
}