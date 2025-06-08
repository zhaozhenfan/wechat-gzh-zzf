package com.zzf.wechatgzhzzf.demos.entity;

public class Source {
    private String title;
    private String url;
    private Integer isType;

    public Source(String title, String url, Integer isType) {
        this.title = title;
        this.url = url;
        this.isType = isType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getIsType() {
        return isType;
    }

    public void setIsType(Integer isType) {
        this.isType = isType;
    }
}
