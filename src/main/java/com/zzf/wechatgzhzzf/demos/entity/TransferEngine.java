package com.zzf.wechatgzhzzf.demos.entity;

public enum TransferEngine {
    BAIDU(2),
    QUARK(0);

    private final int value;

    TransferEngine(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
