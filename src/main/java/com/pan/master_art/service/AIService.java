package com.pan.master_art.service;

import com.pan.master_art.entity.AiRequest;
import com.pan.master_art.entity.AiResponse;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class AIService {
    @Autowired
    private OkHttpClient okHttpClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.model}")
    private String model;

    public String getAiResponse(String Message) throws Exception {
        // 1. 构造请求体
        AiRequest request = new AiRequest();
        request.setModel(model);
        request.setMessages(List.of(
                new AiRequest.Message("user", Message)  // 第一个参数是角色，第二个是内容
        ));
        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(request),
                MediaType.parse("application/json")
        );

        // 2. 构造 HTTP 请求
        Request httpRequest = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey) // 注入 API Key
                .addHeader("Content-Type", "application/json")
                .build();

        // 3. 执行请求并解析响应
        try (Response response = okHttpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("API 请求失败: " + response.code());
            }

            AiResponse aiResponse = objectMapper.readValue(
                    response.body().string(),
                    AiResponse.class
            );

            return aiResponse.getChoices().get(0).getMessage().getContent();
        }
    }
}