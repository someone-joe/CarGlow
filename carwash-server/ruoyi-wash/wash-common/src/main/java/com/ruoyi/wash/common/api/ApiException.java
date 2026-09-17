package com.ruoyi.wash.common.api;

/**
 * 业务异常 —— 携带契约错误码，由全局异常处理转成 ApiResult 返回。
 * 禁止直接 throw new RuntimeException(msg)，那会让前端拿不到错误码。
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String msg) {
        super(msg);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
