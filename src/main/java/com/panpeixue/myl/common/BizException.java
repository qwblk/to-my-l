package com.panpeixue.myl.common;

/**
 * 业务异常 —— 用 (code, msg) 直接表达响应。
 *
 * 区别于 IllegalArgumentException(那个固定走 400)，BizException 让 service/controller
 * 能精确抛出 404、403、409 等业务语义。被 GlobalExceptionHandler 捕获后原样下发。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /** 资源不存在 / 已软删，对调用方而言等价于「找不到」 */
    public static BizException notFound(String msg) {
        return new BizException(404, msg);
    }

    /** 鉴权通过但无权操作（比如删别人的资源） */
    public static BizException forbidden(String msg) {
        return new BizException(403, msg);
    }
}
