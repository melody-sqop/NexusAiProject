package com.cmt.NexusAi.common.enums;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum ViolationTag {
    PASS("PASS", 0),
    P1_CONTACT("P1_CONTACT", 25),
    AI_REJECT("AI_REJECT", 50),
    REPORT_HIT("REPORT_HIT", 25),
    P0_BLACKLIST("P0_BLACKLIST", 100);

    private final String tag;
    private final int score;

    ViolationTag(String tag, int score) {
        this.tag = tag;
        this.score = score;
    }

    public String getTag() { return tag; }
    public int getScore() { return score; }

    /**
     * 安全转换方法，防止缓存中的脏标签导致系统崩溃
     */
    public static ViolationTag safeFromTag(String tagStr) {
        if (tagStr == null) return AI_REJECT; // 兜底：标签为空默认违规
        for (ViolationTag vt : values()) {
            if (vt.tag.equals(tagStr)) {
                return vt;
            }
        }
        log.error("[枚举转换] 发现未知违规标签: {}, 降级为AI_REJECT", tagStr);
        return AI_REJECT; // 兜底：未知标签默认违规（宁杀错不放过）
    }
}