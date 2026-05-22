package com.cmt.NexusAi.modules.notice.constant;

public class NoticeConstant {
    public static final String AGGREGATE_WINDOWS_KEY = "notice:aggregate:windows";
    public static final String AGGREGATE_DETAIL_PREFIX = "notice:aggregate:detail:";
    public static final String AGGREGATE_LOCK_PREFIX = "notice:aggregate:lock:";

    public static final long WINDOW_SLIDE_SECONDS = 60L;
    public static final long WINDOW_MAX_LIFESPAN_SECONDS = 300L;
    // 移除 WINDOW_MAX_CAPACITY，应用层不再判断容量
    public static final long DETAIL_KEY_TTL_SECONDS = 7200L;
    public static final long SCHEDULER_INTERVAL_MS = 10000L;
}