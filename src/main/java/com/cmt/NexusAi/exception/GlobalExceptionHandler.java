package com.cmt.NexusAi.exception;

import com.cmt.NexusAi.common.BaseResponse;
import com.cmt.NexusAi.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Hidden  // 🔥 关键：隐藏异常处理器，防止文档解析异常
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获所有 RuntimeException，分类返回结果
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> handleRuntimeException(RuntimeException e) {
        String msg = e.getMessage();
        log.error("业务异常：{}", msg);

        // 1. 参数错误 → 返回 400
        if (msg.equals("参数错误")) {
            return ResultUtils.error(400, msg);
        }
        // 2. 已点赞/重复操作 → 返回 409
        else if (msg.equals("用户已点赞")) {
            return ResultUtils.error(400, msg);
        }
        // 3. 未登录 → 返回 401
        else if (msg.equals("未登录")) {
            return ResultUtils.error(400, msg);
        }
        // 4. 其他错误 → 默认 500
        else {
            return ResultUtils.error(500, msg);
        }
    }

    /**
     * 参数校验失败
     * @param e 具体校验失败的异常
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        // 获取第一个校验失败的提示信息
        String errorMsg = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResultUtils.error(400,errorMsg);
    }

}
