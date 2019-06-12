package com.hzc.secKill.Result;

public class ResultUtil<T> {

    private int code;

    private String msg;

    private T data;

    private ResultUtil(T data) {
        this.code = 0;
        this.msg = "success";
        this.data = data;
    }

    private ResultUtil(CodeMsg cm) {
        if (null == cm) {
            return ;
        }
        this.code = cm.getCode();
        this.msg = cm.getMsg();
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

    public static <T> ResultUtil<T> success(T data) {
        return new ResultUtil<T>(data);
    }

    public static <T> ResultUtil<T> error(CodeMsg cm) {
        return new ResultUtil<T>(cm);
    }

}
