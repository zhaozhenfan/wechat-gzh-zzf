package com.zzf.wechatgzhzzf.demos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "api")
public class SearchConfig {

    //通用配置
    private Integer connectionTimeout;
    private Integer readTimeout;

    // 备用url前缀
    private String urlPrefix;

    private String[] SEARCH_API_URLS;

    @PostConstruct  // 在 Bean 初始化后自动构建 URL 数组
    public void init() {
        SEARCH_API_URLS = new String[] {
                "http://" + urlPrefix + "/v/api/search",
                "http://" + urlPrefix + "/v/api/getDJ",
                "http://" + urlPrefix + "/v/api/getJuzi",
                "http://" + urlPrefix + "/v/api/getXiaoyu"
        };
    }

    public String[] getSearchApiUrls() {
        return SEARCH_API_URLS;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }
}
