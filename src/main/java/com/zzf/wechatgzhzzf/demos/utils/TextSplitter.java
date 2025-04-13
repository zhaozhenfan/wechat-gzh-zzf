package com.zzf.wechatgzhzzf.demos.utils;

import java.util.ArrayList;
import java.util.List;

public class TextSplitter {
    /**
     * 将长文本按照每N个段落分割
     * @param content 原始文本内容
     * @param paragraphsPerMessage 每条消息包含的段落数
     * @return 分割后的文本列表
     */
    public static List<String> splitByParagraphs(String content, int paragraphsPerMessage) {
        List<String> result = new ArrayList<>();

        // 按两个换行符分割成段落
        String[] paragraphs = content.split("\\n\\s*\\n");

        StringBuilder currentMessage = new StringBuilder();
        int paragraphCount = 0;

        for (String paragraph : paragraphs) {
            // 跳过空段落
            if (paragraph.trim().isEmpty()) {
                continue;
            }

            if (currentMessage.length() > 0) {
                currentMessage.append("\n\n"); // 添加段落分隔
            }
            currentMessage.append(paragraph);
            paragraphCount++;

            // 达到指定段落数或最后一个段落时，添加到结果
            if (paragraphCount >= paragraphsPerMessage || paragraph.equals(paragraphs[paragraphs.length - 1])) {
                result.add(currentMessage.toString());
                currentMessage = new StringBuilder();
                paragraphCount = 0;
            }
        }

        return result;
    }
}
