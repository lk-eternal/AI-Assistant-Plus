package lk.eternal.ai.service;

public class ChatGPT3_5Service extends ChatGPTService {

    public ChatGPT3_5Service(String openaiApiKey, String openaiApiUrl) {
        super(openaiApiKey, openaiApiUrl, "gpt-3.5-turbo-1106");
    }
}