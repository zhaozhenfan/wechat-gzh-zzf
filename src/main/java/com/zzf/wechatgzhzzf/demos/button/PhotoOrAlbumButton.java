package com.zzf.wechatgzhzzf.demos.button;

public class PhotoOrAlbumButton extends AbstractButton{

    public PhotoOrAlbumButton(String name,String key) {
        super(name);
        this.type = "pic_photo_or_album";
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
