package com.zzf.wechatgzhzzf.demos.Service;

import com.zzf.wechatgzhzzf.demos.config.SearchConfig;
import com.zzf.wechatgzhzzf.demos.util.AdCleaner;
import com.zzf.wechatgzhzzf.demos.web.WxController;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ApiClient {

    @Autowired
    private SearchConfig searchConfig;

    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);
    /**
     * 处理用户输入，判断是否触发搜索
     * @param input 用户输入的字符串
     * @return 搜索结果或提示信息
     */
    public String keywordSearch(String input) {
        // 检查是否以 "帮我搜索" 开头（严格匹配，包含空格校验）
        if (input != null && input.startsWith(searchConfig.getKeyWord())) {
            // 移除 "帮我搜索" 并 trim 后传入
            String searchQuery = input.substring(searchConfig.getKeyWord().length()).trim();
            return callKobApiAndExtractResults(searchQuery);
        } else {
            return searchConfig.getKeywordTip();
        }
    }

    public String callKobApiAndExtractResults(String input) {
        String token = null;
        try {
            // 第一步：获取token（所有接口共用同一个token）
            String tokenUrl = "http://" + searchConfig.getUrlPrefix() + "/v/api/getToken";
            String tokenResponse = sendGetRequest(tokenUrl,null);
            JSONObject tokenJson = new JSONObject(tokenResponse);
            token = tokenJson.getString("token");
        } catch (Exception e) {
            return searchConfig.getFailureMessage();
        }

        // 依次尝试各个搜索接口
        for (String apiUrl : searchConfig.getSearchApiUrls()) {
            try {
                // 准备请求体
                JSONObject requestBody = new JSONObject();
                requestBody.put("name", input);
                requestBody.put("token", token);

                // 发送请求
                String searchResponse = sendPostRequest(apiUrl, requestBody.toString());

                // 提取结果
                List<String> answers = extractAnswers(searchResponse);
                Map<String, String> ansResults = processAnswers(answers);

                // 检查Map是否为空
                if (ansResults == null || ansResults.isEmpty()) {
                    continue;
                }

                StringBuilder resultBuilder = new StringBuilder();

                // 添加成功前缀
                resultBuilder.append(searchConfig.getSuccessPrefix())
                        .append("\n");

                // 遍历Map并添加键值对
                for (Map.Entry<String, String> entry : ansResults.entrySet()) {
                    resultBuilder.append(entry.getKey())  // 添加键
                            .append("\n")            // 换行
                            .append(entry.getValue()) // 添加值
                            .append("\n");           // 换行
                }

                // 添加成功后缀
                resultBuilder.append(searchConfig.getSuccessSuffix());

                return resultBuilder.toString();
            } catch (Exception e) {
                // 当前API查询失败，继续尝试下一个
                continue;
            }
        }

        // 所有API都尝试过了，没有结果
        return searchConfig.getFailureMessage();
    }

    private List<String> extractAnswers(String apiResponse) throws JSONException {
        JSONObject responseJson = new JSONObject(apiResponse);
        int maxResults = searchConfig.getMaxResults(); // 获取最大结果数
        List<String> answerList = new ArrayList<>();

        // 处理包含list字段的响应（search接口）
        if (responseJson.has("list")) {
            JSONArray list = responseJson.getJSONArray("list");
            int count = 0;
            for (int i = 0; i < list.length() && count < maxResults; i++) {
                JSONObject item = list.getJSONObject(i);
                String answerText = item.optString("answer", "").trim();

                // 从文本中提取所有URL链接
                List<String> urls = extractUrlsFromText(answerText);
                for (String url : urls) {
                    answerList.add(url);
                }
                count++;
            }
            return answerList;
        }

        // 处理单结果响应（其他接口）
        String answerText = responseJson.optString("answer", "").trim();
        List<String> urls = extractUrlsFromText(answerText);
        int count = 0;
        for (String url : urls) {
            if (count >= maxResults) break;
            answerList.add(url);
            count++;
        }

        return answerList;
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

    private Map<String, String> processAnswers(List<String> answers) {
        Map<String, String> resultMap = new HashMap<>();

        for (String answer : answers) {
            try {
                // 0. 检查是否包含baidu或quark - 如果不包含则跳过
                if (!answer.contains("baidu") && !answer.contains("quark")) {
                    System.out.println("跳过非百度/夸克链接: " + answer);
                    continue;
                }

                // 1. 解析code参数
                String code = extractCodeFromUrl(answer);

                // 2. 确定isType参数
                int isType = determineIsType(answer);

                // 3. 构建请求参数
                Map<String, String> params = new HashMap<>();
                params.put("url", answer);
                params.put("expired_type", "2");
                params.put("isType", String.valueOf(isType));
                params.put("isSave", "1");

                if (code != null && !code.isEmpty()) {
                    params.put("code", code);
                }

                // 4. 发送API请求
                String apiResponse = sendGetRequest("https://www.zzfdip.cyou/api/open/transfer", params);

                // 5. 解析API响应
                JSONObject responseJson = new JSONObject(apiResponse);
                if (responseJson.getInt("code") == 200) {
                    JSONObject data = responseJson.getJSONObject("data");
                    String title = data.getString("title");
                    String shareUrl = data.getString("share_url");

                    // 6. 存入结果Map
                    resultMap.put(AdCleaner.cleanAdvertisements(title), shareUrl);
                } else {
                    // 处理API错误响应
                    String errorMsg = responseJson.getString("message");
                    logger.error("API调用失败: " + errorMsg + " | URL: " + answer);
                }
            } catch (Exception e) {
                // 处理异常（记录日志等）
                logger.error("处理答案时出错: " + answer);
            }
        }

        return resultMap;
    }

    // 从URL中提取code参数
    private String extractCodeFromUrl(String url) {
        int pwdIndex = url.indexOf("pwd=");
        if (pwdIndex != -1) {
            String afterPwd = url.substring(pwdIndex + 4);
            int endIndex = afterPwd.indexOf('&');
            if (endIndex == -1) endIndex = afterPwd.length();
            return afterPwd.substring(0, endIndex);
        }
        return null;
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
        String result = apiClient.callKobApiAndExtractResults(input);
        System.out.println(result);
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
