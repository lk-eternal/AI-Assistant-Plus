package lk.eternal.ai.service;

public class ChatGPT3_5Service extends ChatGPTService {

    public ChatGPT3_5Service(String openaiApiKey) {
        super(openaiApiKey, "gpt-3.5-turbo-1106");
    }
}