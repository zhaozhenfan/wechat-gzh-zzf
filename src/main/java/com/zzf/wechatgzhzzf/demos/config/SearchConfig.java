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

    //提示信息
    private String successPrefix;
    private String successSuffix;
    private String failureMessage;

    private String keyWord;

    private String keywordTip;

    private Integer maxResults;

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    public String getKeywordTip() {
        return keywordTip;
    }

    public void setKeywordTip(String keywordTip) {
        this.keywordTip = keywordTip;
    }

    public String getKeyWord() {
        return keyWord;
    }

    public void setKeyWord(String keyWord) {
        this.keyWord = keyWord;
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

    public String getSuccessPrefix() {
        return successPrefix;
    }

    public void setSuccessPrefix(String successPrefix) {
        this.successPrefix = successPrefix;
    }

    public String getSuccessSuffix() {
        return successSuffix;
    }

    public void setSuccessSuffix(String successSuffix) {
        this.successSuffix = successSuffix;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }
}
