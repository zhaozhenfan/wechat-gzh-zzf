package com.zzf.wechatgzhzzf.demos.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "button")
public class ButtonConfig {
    //按钮配置
    private String button1Content;
    private String button2Content;
    private String button3Content;

    public String getButton1Content() {
        return button1Content;
    }

    public void setButton1Content(String button1Content) {
        this.button1Content = button1Content;
    }

    public String getButton2Content() {
        return button2Content;
    }

    public void setButton2Content(String button2Content) {
        this.button2Content = button2Content;
    }

    public String getButton3Content() {
        return button3Content;
    }

    public void setButton3Content(String button3Content) {
        this.button3Content = button3Content;
    }
}
