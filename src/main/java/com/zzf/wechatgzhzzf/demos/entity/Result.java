package com.zzf.wechatgzhzzf.demos.entity;

import java.util.List;

public class Result {
    private Integer code;
    private String message;
    private List<Source> data;

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result(Integer code, String message, List<Source> data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Source> getData() {
        return data;
    }

    public void setData(List<Source> data) {
        this.data = data;
    }
}
