package lk.eternal.ai.service;

public class ChatGPT4Service extends ChatGPTService {

    public ChatGPT4Service(String openaiApiKey, String openaiApiUrl) {
        super(openaiApiKey, openaiApiUrl, "gpt-4-1106-preview");
    }
}
