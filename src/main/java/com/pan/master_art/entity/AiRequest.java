package com.pan.master_art.entity;
// AiRequest.java
import lombok.Data;
import java.util.List;

@Data
public class AiRequest {
    private String model;
    private List<Message> messages;

    @Data
    public static class Message {
        private String role;  // "user" 或 "assistant"
        private String content;

        // 添加这个构造函数
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}

