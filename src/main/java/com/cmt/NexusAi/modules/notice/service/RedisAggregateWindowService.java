package com.cmt.NexusAi.modules.notice.service;

import com.cmt.NexusAi.modules.notice.dto.NotificationEvent;

public interface RedisAggregateWindowService {
    public void addToWindow(NotificationEvent event);
}
