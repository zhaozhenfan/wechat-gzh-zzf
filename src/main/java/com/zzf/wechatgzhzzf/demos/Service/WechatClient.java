package com.zzf.wechatgzhzzf.demos.Service;

import com.zzf.wechatgzhzzf.demos.config.SearchConfig;

import com.zzf.wechatgzhzzf.demos.token.WechatAccessToken;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WechatClient {

    @Autowired
    private ApiClient apiClient;

    private static final String WECHAT_APP_ID = "wx6b2d8c7a50169ddc";
    private static final String WECHAT_APP_SECRET = "1cff981ee7a0e2da255a7f4e7083d68c";
    // 使用volatile保证可见性
    private volatile WechatAccessToken accessToken = new WechatAccessToken();

    // 锁对象
    private final Object tokenLock = new Object();

    private void getToken() throws Exception {
        String urlString = "https://api.weixin.qq.com/cgi-bin/token";
        //构建请求参数
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "client_credential");
        params.put("appid", WECHAT_APP_ID);
        params.put("secret", WECHAT_APP_SECRET);
        String tokenResponse = apiClient.sendGetRequest(urlString,params);
        JSONObject tokenJson = new JSONObject(tokenResponse);
        String token = tokenJson.getString("access_token");
        System.out.println(token);
        long expiresIn = tokenJson.getLong("expires_in");
        // 创建新对象而不是修改原有对象（避免部分更新的问题）
        WechatAccessToken newToken = new WechatAccessToken();
        newToken.setToken(token);
        newToken.setExpireTime(expiresIn);
        this.accessToken = newToken;
    }

    /**
     * 线程安全的获取AccessToken方法
     */
    public String getWechatAccessToken() throws Exception {
        WechatAccessToken current = this.accessToken;
        if (current == null || current.isExpired()) {
            synchronized (tokenLock) {
                // 双重检查
                current = this.accessToken;
                if (current == null || current.isExpired()) {
                    getToken();
                }
            }
        }
        return this.accessToken.getToken();
    }

    public void buildButtons(String json) throws Exception {
        String accessToken = getWechatAccessToken();
        String urlString = String.format("https://api.weixin.qq.com/cgi-bin/menu/create?access_token=%s",
                accessToken);

        String result = apiClient.sendPostRequest(urlString, json);
        System.out.println(result);
    }

}
