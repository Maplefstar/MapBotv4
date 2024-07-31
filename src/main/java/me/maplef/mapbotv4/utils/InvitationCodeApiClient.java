package me.maplef.mapbotv4.utils;

import me.maplef.mapbotv4.managers.ConfigManager;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;

public class InvitationCodeApiClient {

    static ConfigManager configManager = new ConfigManager();
    static FileConfiguration config = configManager.getConfig();

    private static final String API_URL = "https://invitationcode.nekocafe.moe/api/check";
    private static final String ACCESS_TOKEN = config.getString("access-token");

    public static ApiResponse checkInvitationCode(long qq, String code, boolean markUsed) throws IOException {
        // 构建请求体JSON字符串
        ApiRequest apiRequest = new ApiRequest();
        apiRequest.setQQ(qq);
        apiRequest.setCode(code);

        // 发送POST请求并获取响应
        HttpResponse response = sendPostRequest(apiRequest);
        // 处理响应
        return handleResponse(response);
    }

    private static HttpResponse sendPostRequest(ApiRequest apiRequest) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(String.format("%s?qq=%s&code=%s",API_URL,apiRequest.getQQ(),apiRequest.getCode()));

        // 设置请求头
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Authorization", "Bearer " + InvitationCodeApiClient.ACCESS_TOKEN);

        return httpClient.execute(httpPost);
    }


    private static ApiResponse handleResponse(HttpResponse response) throws IOException {
        ApiResponse apiResponse = new ApiResponse();
        // 获取响应状态码
        int statusCode = response.getStatusLine().getStatusCode();
        apiResponse.setCode(statusCode);

        // 读取响应内容
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String responseBody = EntityUtils.toString(entity);
            apiResponse.setBody(responseBody);
        }

        return apiResponse;
    }

    public static class ApiResponse {
        private int code;
        private String body;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    public static class ApiRequest {
        private String code;
        private long qq;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public long getQQ() {
            return qq;
        }

        public void setQQ(long qq) {
            this.qq = qq;
        }
    }

}
