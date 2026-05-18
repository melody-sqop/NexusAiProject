package com.cmt.NexusAi.common.enums;

public enum ViolationTag {
    PASS("PASS", 0),
    P1_CONTACT("P1_CONTACT", 25),
    AI_REJECT("AI_REJECT", 50),
    P0_BLACKLIST("P0_BLACKLIST", 100),
    REPORT_HIT("REPORT_HIT", 25);

    private final String tag;
    private final int score;

    ViolationTag(String tag, int score) {
        this.tag = tag;
        this.score = score;
    }

    public String getTag() {
        return tag;
    }

    public int getScore() {
        return score;
    }
}