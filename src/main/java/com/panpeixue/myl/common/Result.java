package com.panpeixue.myl.common;

import java.io.Serializable;

public class Result<T> implements Serializable {

    private int code;
    private String msg;
    private T data;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public static <T> Result<T> ok() { return ok(null); }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok(String msg, T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = msg;
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(String msg) { return error(500, msg); }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}