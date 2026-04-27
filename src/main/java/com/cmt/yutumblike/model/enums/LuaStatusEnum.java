package com.cmt.yutumblike.model.enums;

import lombok.Getter;

// Lua脚本执行结果枚举
@Getter
public enum LuaStatusEnum {
    // 成功
    SUCCESS(1L),
    // 失败
    FAIL(-1L),
    ;

    private final long value;

    LuaStatusEnum(long value) {
        this.value = value;
    }
}

