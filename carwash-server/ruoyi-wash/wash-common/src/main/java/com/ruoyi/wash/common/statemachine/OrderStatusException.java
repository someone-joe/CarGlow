package com.ruoyi.wash.common.statemachine;

/**
 * 状态流转异常。所有非法流转都抛它，调用方统一转错误码，禁止吞掉。
 */
public class OrderStatusException extends RuntimeException {

    public OrderStatusException(String message) {
        super(message);
    }

    public OrderStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
