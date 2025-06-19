package com.zzf.wechatgzhzzf.demos.entity;

import java.util.List;

public class Result {
    private Integer code;
    private String msg;
    private List<Source> data;

    public Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Result(Integer code, String msg, List<Source> data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
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

    public List<Source> getData() {
        return data;
    }

    public void setData(List<Source> data) {
        this.data = data;
    }
}
