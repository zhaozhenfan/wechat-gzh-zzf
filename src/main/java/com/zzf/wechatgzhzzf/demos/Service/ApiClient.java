package com.zzf.wechatgzhzzf.demos.Service;

import com.zzf.wechatgzhzzf.demos.config.SearchConfig;
import com.zzf.wechatgzhzzf.demos.entity.Source;
import com.zzf.wechatgzhzzf.demos.entity.TransferEngine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ApiClient {

    @Autowired
    private SearchConfig searchConfig;

    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);

    public List<Source> callKobApiAndExtractResults(String input, Integer isType) {
        String token = null;
        List<Source> res = new ArrayList<>();
        try {
            // 第一步：获取token（所有接口共用同一个token）
            String tokenUrl = "http://" + searchConfig.getUrlPrefix() + "/v/api/getToken";
            String tokenResponse = sendGetRequest(tokenUrl,null);
            JSONObject tokenJson = new JSONObject(tokenResponse);
            token = tokenJson.getString("token");
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        List<Source> firstPairResults = new ArrayList<>();  // 存储每个响应第一个问答对的所有结果
        List<Source> otherResults = new ArrayList<>();      // 存储其他问答对的结果
        for (String apiUrl : searchConfig.getSearchApiUrls()) {
            try {
                // 准备请求体
                JSONObject requestBody = new JSONObject();
                requestBody.put("name", input);
                requestBody.put("token", token);

                // 发送请求
                String searchResponse = sendPostRequest(apiUrl, requestBody.toString());

                // 处理响应
                JSONObject responseJson = new JSONObject(searchResponse);
                if (responseJson.has("list")) {
                    JSONArray list = responseJson.getJSONArray("list");
                    boolean isFirstItem = true;

                    for (int i = 0; i < list.length(); i++) {
                        JSONObject item = list.getJSONObject(i);
                        String questionTextOrigin = item.optString("question", "").trim();
                        String questionText = (questionTextOrigin == null || questionTextOrigin.isEmpty()) ? "未知" : questionTextOrigin;
                        String answerText = item.optString("answer", "").trim();

                        // 从文本中提取所有URL链接
                        List<String> links = extractUrlsFromText(answerText);
                        List<Source> currentSources = new ArrayList<>();

                        for (String link : links) {
                            if (isType == TransferEngine.BAIDU.getValue() && !link.contains("baidu")) {
                                continue;
                            }
                            if (isType == TransferEngine.QUARK.getValue() && !link.contains("quark")) {
                                continue;
                            }
                            currentSources.add(new Source(questionText, link, determineIsType(link)));
                        }

                        if (isFirstItem && !currentSources.isEmpty()) {
                            // 当前响应的第一个问答对的所有结果
                            firstPairResults.addAll(currentSources);
                            isFirstItem = false;
                        } else {
                            // 其他问答对的结果
                            otherResults.addAll(currentSources);
                        }
                    }
                }
            } catch (Exception e) {
                // 当前API查询失败，继续尝试下一个
                continue;
            }
        }

        // 合并结果：先放所有响应第一个问答对的结果，再放其他结果
        res.addAll(firstPairResults);
        res.addAll(otherResults);
        return res;
    }

    // 从文本中提取所有URL链接
    private List<String> extractUrlsFromText(String text) {
        List<String> urls = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return urls;
        }

        // 正则表达式匹配URL
        String regex = "(https?://[\\w.-]+(?:/\\S*)?)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String url = matcher.group(1).trim();
            // 确保URL是有效的
            if (url.startsWith("http://") || url.startsWith("https://")) {
                urls.add(url);
            }
        }

        return urls;
    }

    // 根据URL内容确定isType参数
    private int determineIsType(String url) {
        if (url.contains("baidu")) {
            return 2;
        } else if (url.contains("quark")) {
            return 0;
        }
        // 默认值（根据您的业务需求调整）
        return 0;
    }

    public String sendGetRequest(String baseUrl, Map<String, String> params) throws Exception {
        // 构建带参数的完整URL
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        if (params != null && !params.isEmpty()) {
            urlBuilder.append("?");
            boolean firstParam = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!firstParam) {
                    urlBuilder.append("&");
                }
                urlBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                firstParam = false;
            }
        }

        String fullUrl = urlBuilder.toString();
        System.out.println("完整请求URL: " + fullUrl); // 调试日志

        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            // 使用URI解决非法字符问题
            URI uri = new URI(fullUrl);
            URL url = uri.toURL();

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(searchConfig.getConnectionTimeout());
            connection.setReadTimeout(searchConfig.getReadTimeout());

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = reader.readLine()) != null) {
                    response.append(inputLine);
                }
                return response.toString();
            } else {
                throw new RuntimeException("GET请求失败，响应码: " + responseCode);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // 关闭时的异常可以忽略
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String sendPostRequest(String urlString, String requestBody) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(searchConfig.getConnectionTimeout());
            connection.setReadTimeout(searchConfig.getReadTimeout());
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = reader.readLine()) != null) {
                    response.append(inputLine);
                }
                return response.toString();
            } else {
                throw new RuntimeException("POST请求失败，响应码: " + responseCode);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // 关闭时的异常可以忽略
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 上传图片到微信接口（multipart/form-data）
     * @param urlString 请求地址（含access_token）
     * @param imageFile 图片文件
     * @return 微信API返回的JSON字符串
     */
    public static String uploadImage(String urlString, File imageFile) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            // 边界字符串（用于分隔表单数据）
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            // 设置请求头
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setConnectTimeout(5000); // 连接超时（毫秒）
            connection.setReadTimeout(5000);    // 读取超时（毫秒）
            connection.setDoOutput(true);

            // 构造请求体（multipart/form-data格式）
            try (OutputStream os = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

                // 1. 添加文件部分
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"media\"; filename=\"")
                        .append(imageFile.getName()).append("\"\r\n");
                writer.append("Content-Type: image/jpg\r\n\r\n"); // 根据实际文件类型修改
                writer.flush();

                // 写入文件二进制数据
                try (FileInputStream fis = new FileInputStream(imageFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.flush();
                }

                // 2. 结束边界
                writer.append("\r\n--").append(boundary).append("--\r\n");
                writer.flush();
            }

            // 获取响应
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            } else {
                throw new RuntimeException("上传失败，响应码: " + responseCode);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static void main(String[] args) {
        // 测试接口
        String input = "不可思议星期二";
        ApiClient apiClient = new ApiClient();
        List<Source> sources = apiClient.callKobApiAndExtractResults(input,2);
        System.out.println(sources);
//        String accessToken = "90_6TARaUcHjJgqAjFT_3ENPvtV0bYqUvja5S16CSlwhQPRbK6BBTboxZg1DZCL_JIaFqvupzIfzyvoKJbex7JxaOGtiwc7fw9F2bvkUaZ26Xw8kZ_JBB31APh8hWwGHXbAGAWXN"; // 替换为你的 access_token
//        String imagePath = "C:\\Users\\84053\\Desktop\\dlaopic.png"; // 替换为图片路径
//        String type = "image";
//        String url = String.format("https://api.weixin.qq.com/cgi-bin/material/add_material?access_token=%s&type=%s",accessToken,type);
//        try {
//            String s = uploadImage(url, new File(imagePath));
//            System.out.println(s);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }
}
