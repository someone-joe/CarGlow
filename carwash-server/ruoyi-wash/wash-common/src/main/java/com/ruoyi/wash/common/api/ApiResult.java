package com.ruoyi.wash.common.api;

/**
 * 统一响应体 —— 与 openapi.yaml 的 Result schema 逐字段一致。
 *
 * <p>注意：code = 0 表示成功（与若依 AjaxResult 的 200 不同）。
 * /api/v1/**（三端小程序）一律用本类；后台管理沿用若依 AjaxResult，两套体系并存，禁止混用。
 */
public class ApiResult<T> {

    private int code;
    private String msg;
    private T data;
    private String traceId;

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 0;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMsg());
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode, String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.code = errorCode.numeric();
        r.msg = msg;
        return r;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
