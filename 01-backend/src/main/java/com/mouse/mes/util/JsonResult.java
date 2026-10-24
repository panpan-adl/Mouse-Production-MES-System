package com.mouse.mes.util;

import java.io.Serializable;

public class JsonResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private T data;

    public JsonResult() {
        super();
    }

    public JsonResult(Integer code, String msg) {
        super();
        this.code = code;
        this.msg = msg;
    }

    public JsonResult(Integer code, String msg, T data) {
        super();
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> JsonResult<T> success() {
        return new JsonResult<>(200, "success");
    }

    public static <T> JsonResult<T> success(T data) {
        return new JsonResult<>(200, "success", data);
    }

    public static <T> JsonResult<T> success(T data, String msg) {
        return new JsonResult<>(200, msg, data);
    }

    public static <T> JsonResult<T> error() {
        return new JsonResult<>(500, "error");
    }

    public static <T> JsonResult<T> error(String msg) {
        return new JsonResult<>(500, msg);
    }

    public static <T> JsonResult<T> error(Integer code, String msg) {
        return new JsonResult<>(code, msg);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
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
}
