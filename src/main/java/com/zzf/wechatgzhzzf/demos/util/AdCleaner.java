package com.zzf.wechatgzhzzf.demos.util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AdCleaner {
    public static String cleanAdvertisements(String input) {
        if (input == null || input.isEmpty()) {
            return "未命名";
        }

        // 定义广告关键词（可扩展）
        String adKeywords = "公众号|微信|wechat|推广|广告|二维码|加群|资源获取|领取福利|获取资源";

        // 模式1：匹配包含广告关键词的括号内容（包括括号）
        Pattern bracketPattern = Pattern.compile(
                "\\[[^\\]]*(" + adKeywords + ")[^\\]]*\\]",
                Pattern.CASE_INSENSITIVE
        );

        // 模式2：匹配广告关键词及其后面的内容（直到遇到空格或标点）
        Pattern keywordPattern = Pattern.compile(
                "(" + adKeywords + ")[\\s:：]*(\\S+)?",
                Pattern.CASE_INSENSITIVE
        );

        // 模式3：匹配可能残留的广告格式（如：@xxx 或 vx:xxx）
        Pattern miscPattern = Pattern.compile(
                "[@vV][xX]\\w+|添加?[\\w\\u4e00-\\u9fa5]{2,8}",
                Pattern.CASE_INSENSITIVE
        );

        // 先移除括号内的广告内容
        String result = bracketPattern.matcher(input).replaceAll("");

        // 再移除广告关键词及其相关文本
        result = keywordPattern.matcher(result).replaceAll("");

        // 移除其他常见广告格式
        result = miscPattern.matcher(result).replaceAll("");

        // 清理残留的标点和空格
        result = result.replaceAll("\\[\\s*\\]", "")  // 空括号
                .replaceAll("\\s+", " ")       // 多个空格合并
                .replaceAll("^[\\s:：]+|[\\s:：]+$", ""); // 首尾标点

        return result.trim().isEmpty() ? "未命名" : result;
    }
}
