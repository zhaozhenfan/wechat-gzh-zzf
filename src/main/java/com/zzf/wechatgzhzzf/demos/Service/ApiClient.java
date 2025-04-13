package com.zzf.wechatgzhzzf.demos.Service;

import com.zzf.wechatgzhzzf.demos.config.SearchConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApiClient {

    @Autowired
    private SearchConfig searchConfig;

    /**
     * 综合查询方法 - 先尝试飞书接口，失败后再尝试原API接口
     * @param input 查询关键词
     * @return 查询结果
     */
    private String combinedSearch(String input) {
        // 1. 先尝试飞书接口查询
        String fsResult = callFSApiAndExtractResults(input);

        // 检查飞书接口是否返回了有效结果（不是失败提示且不为空）
        if (isValidResult(fsResult)) {
            return fsResult;
        }

        // 2. 飞书接口查询失败或无结果，尝试原API接口
        String apiResult = callKobApiAndExtractResults(input);

        // 返回原API接口的结果（无论是否成功）
        return apiResult;
    }

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
            return combinedSearch(searchQuery);
        } else {
            return searchConfig.getKeywordTip();
        }
    }

    /**
     * 判断结果是否有效
     * @param result 查询结果
     * @return 是否有效
     */
    private boolean isValidResult(String result) {
        // 结果为空
        if (result == null || result.trim().isEmpty()) {
            return false;
        }

        // 结果是失败提示
        if (result.equals(searchConfig.getFailureMessage())) {
            return false;
        }

        // 结果以成功前缀开头
        return result.startsWith(searchConfig.getSuccessPrefix());
    }

    public String callFSApiAndExtractResults(String input) {
        try {
            // 第一步：获取飞书tenant_access_token
            String fsToken = getFSToken();
            if (fsToken == null) {
                return searchConfig.getFailureMessage();
            }

            // 第二步：使用token查询飞书接口
            String searchUrl = String.format("https://open.feishu.cn/open-apis/bitable/v1/apps/%s/tables/%s/records/search?page_size=%d",
                    searchConfig.getAppToken(), searchConfig.getTableId(), searchConfig.getPageSize());

            // 使用模板构建请求体（替换占位符）
            String requestBody = String.format(searchConfig.getRequestBodyTemplate(), input);

            // 发送请求
            String searchResponse = sendFSRequest(searchUrl, fsToken, requestBody);

            // 提取结果
            String result = extractFSResult(searchResponse);

            // 处理结果
            if (result != null && !result.trim().isEmpty()) {
                return searchConfig.getSuccessPrefix() + result + searchConfig.getSuccessSuffix();
            } else {
                return searchConfig.getFailureMessage();
            }
        } catch (Exception e) {
            return searchConfig.getFailureMessage();
        }
    }

    private String getFSToken() throws Exception {
        String tokenUrl = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";

        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("app_id", searchConfig.getAppId());
        requestBody.put("app_secret", searchConfig.getAppSecret());

        String response = sendPostRequest(tokenUrl, requestBody.toString());
        JSONObject responseJson = new JSONObject(response);

        if (responseJson.getInt("code") == 0) {
            return responseJson.getString("tenant_access_token");
        }
        return null;
    }

    private String sendFSRequest(String urlString, String token, String requestBody) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + token);
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
                throw new RuntimeException("飞书请求失败，响应码: " + responseCode);
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

    private String extractFSResult(String apiResponse) throws JSONException {
        JSONObject responseJson = new JSONObject(apiResponse);
        if (responseJson.getInt("code") != 0 || !responseJson.has("data")) {
            return null;
        }

        JSONObject data = responseJson.getJSONObject("data");
        if (!data.has("items") || data.getJSONArray("items").length() == 0) {
            return null;
        }

        JSONArray items = data.getJSONArray("items");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            JSONObject fields = item.getJSONObject("fields");

            String name = extractFSTextValue(fields, "名字");
            String link = extractFSTextValue(fields, "链接");

            if (!name.isEmpty() || !link.isEmpty()) {
                result.append(name).append("\n").append(link);
                if (i < items.length() - 1) {
                    result.append("\n\n");
                }
            }
        }

        return result.length() > 0 ? result.toString() : null;
    }

    private String extractFSTextValue(JSONObject fields, String fieldName) throws JSONException {
        if (!fields.has(fieldName)) {
            return "";
        }

        JSONObject field = fields.getJSONObject(fieldName);
        if (field.getInt("type") != 1 || !field.has("value")) {
            return "";
        }

        JSONArray values = field.getJSONArray("value");
        if (values.length() == 0) {
            return "";
        }

        return values.getJSONObject(0).getString("text");
    }

    public String callKobApiAndExtractResults(String input) {
        String token = null;
        try {
            // 第一步：获取token（所有接口共用同一个token）
            String tokenUrl = "http://" + searchConfig.getUrlPrefix() + "/v/api/getToken";
            String tokenResponse = sendGetRequest(tokenUrl);
            JSONObject tokenJson = new JSONObject(tokenResponse);
            token = tokenJson.getString("token");
        } catch (Exception e) {
            return searchConfig.getFailureMessage();
        }

        // 存储所有成功的结果
        List<String> validResults = new ArrayList<>();

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
                String result = extractQuestionAndAnswer(searchResponse);

                // 如果结果非空，加入 validResults
                if (result != null && !result.trim().isEmpty()) {
                    validResults.add(result);
                }
            } catch (Exception e) {
                // 当前API查询失败，继续尝试下一个
                continue;
            }
        }

        // 如果有有效结果，拼接返回
        if (!validResults.isEmpty()) {
            String combinedResult = String.join("\n", validResults);  // 用换行符拼接多个结果
            return searchConfig.getSuccessPrefix() + combinedResult + searchConfig.getSuccessSuffix();
        }

        // 所有API都尝试过了，没有有效结果
        return searchConfig.getFailureMessage();
    }

    private String extractQuestionAndAnswer(String apiResponse) throws JSONException {
        JSONObject responseJson = new JSONObject(apiResponse);

        // 检查是否有list字段（适用于search接口）
        if (responseJson.has("list")) {
            JSONArray list = responseJson.getJSONArray("list");
            if (list.length() == 0) {
                return null;
            }

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i);
                String question = item.optString("question", "");
                String answer = item.optString("answer", "");

                if (!question.isEmpty() || !answer.isEmpty()) {
                    result.append(question).append("\n").append(answer);
                    if (i < list.length() - 1) {
                        result.append("\n\n");
                    }
                }
            }
            return result.length() > 0 ? result.toString() : null;
        }

        // 检查其他可能的响应格式（适用于其他接口）
        // 这里可以根据实际API返回格式进行调整
        String question = responseJson.optString("question", "");
        String answer = responseJson.optString("answer", "");

        if (!question.isEmpty() || !answer.isEmpty()) {
            return question + "\n" + answer;
        }

        return null;
    }

    public String sendGetRequest(String urlString) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
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
