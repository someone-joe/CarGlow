package com.ruoyi.wash.member.config;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderStatusException;
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

    /**
     * 状态机拒绝流转 → 契约错误码 B1002（订单状态不允许该操作）。
     * 不映射的话会被若依处理器兜成 500，前端拿不到错误码。
     */
    @ExceptionHandler(OrderStatusException.class)
    public ApiResult<Void> handleOrderStatusException(OrderStatusException e) {
        log.warn("[状态机拒绝] {}", e.getMessage());
        return ApiResult.fail(ErrorCode.B1002, e.getMessage());
    }
}
