package com.cmt.NexusAi.modules.notice.enums;

public enum NoticeRoute {
    // 评论直达，实时性高，直接落库
    HIGH_DIRECT,
    // 点赞聚合，走滑动窗口防轰炸
    AGGREGATE,
    // 大V扇出预留扩展（当前仅作枚举，不写伪逻辑）
    FANOUT
}