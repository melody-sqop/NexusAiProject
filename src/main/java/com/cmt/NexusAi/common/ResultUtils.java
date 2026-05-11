package com.cmt.NexusAi.common;

/**
 * 响应结果工具类
 * 快速生成成功/失败响应
 */
public class ResultUtils {
    // 成功返回
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(200, data, "操作成功");
    }

    // 失败返回
    public static <T> BaseResponse<T> error(int code, String msg) {
        return new BaseResponse<>(code, null, msg);
    }
}