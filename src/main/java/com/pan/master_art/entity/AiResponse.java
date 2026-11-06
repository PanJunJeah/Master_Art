package com.pan.master_art.entity;

// AiResponse.java
import lombok.Data;
import java.util.List;

@Data
public class AiResponse {
    private List<Choice> choices;

    @Data
    public static class Choice {
        private Message message;

        @Data
        public static class Message {
            private String role;
            private String content;
        }
    }
}