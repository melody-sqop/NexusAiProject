package com.cmt.yutumblike.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一响应结果封装类
 * 所有接口都用这个返回，格式统一
 */
@Data
@Schema(description = "统一响应结果")
public class BaseResponse<T> {
    @Schema(description = "响应码：200成功，500失败")
    private int code;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "提示信息")
    private String msg;

    // 构造方法
    public BaseResponse(int code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }
}