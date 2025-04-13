package com.zzf.wechatgzhzzf.demos.button;

public class ClickButton extends AbstractButton{
    public ClickButton(String name, String key) {
        super(name);
        this.type = "click";
        this.key = key;
    }
    private String type;
    private String key;

    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

}
