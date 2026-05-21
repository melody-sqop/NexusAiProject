package com.cmt.NexusAi.modules.notice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmt.NexusAi.modules.notice.dto.NotificationEvent;
import com.cmt.NexusAi.modules.notice.entity.Notice;

/**
 * 通知事务服务接口
 * 专门处理包含幂等校验和落库的复合事务逻辑
 */
public interface NoticeTransactionService extends IService<Notice> {

    /**
     * 处理接收到的通知事件（包含幂等校验和事务落库）
     * @param msgId MQ 消息的唯一 ID
     * @param event 通知事件内容
     * @return true 表示首次处理并落库成功，false 表示重复消息被拦截
     */
    boolean processNotification(String msgId, NotificationEvent event);
}