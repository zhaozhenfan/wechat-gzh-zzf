package com.zzf.wechatgzhzzf.demos.token;

public class WechatAccessToken {
    private String token;
    private long expireTime;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireIn) {
        this.expireTime = System.currentTimeMillis() + expireIn * 1000;
    }

    public boolean isExpired(){
        return System.currentTimeMillis() > this.expireTime;
    }
}
