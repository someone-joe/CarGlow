package com.ruoyi.wash.member.config;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 业务异常处理：只作用于 com.ruoyi.wash 下的 Controller（basePackages 限定），
 * 把 ApiException 转成契约错误码；若依后台接口的异常仍走它自己的处理器，互不干扰。
 *
 * <p>@Order(1) 必须加：否则同样能处理 RuntimeException 的若依处理器可能被优先选中，
 * 业务错误码会被吞成 500（实测如此）。
 */
@RestControllerAdvice(basePackages = "com.ruoyi.wash")
@Order(1)
public class GlobalApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ApiResult<Void> handleApiException(ApiException e) {
        log.warn("[业务异常] code={} msg={}", e.getErrorCode(), e.getMessage());
        return ApiResult.fail(e.getErrorCode(), e.getMessage());
    }
}
