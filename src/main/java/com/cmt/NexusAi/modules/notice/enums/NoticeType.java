package com.cmt.NexusAi.modules.notice.enums;

import lombok.Getter;

@Getter
public enum NoticeType {
    LIKE_BLOG("点赞了你的文章"),
    COMMENT_BLOG("评论了你的文章"),
    REPLY_COMMENT("回复了你的评论");

    private final String defaultTemplate;

    NoticeType(String defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }
}