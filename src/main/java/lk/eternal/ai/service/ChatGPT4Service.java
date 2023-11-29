package lk.eternal.ai.service;

public class ChatGPT4Service extends ChatGPTService {

    public ChatGPT4Service(String openaiApiKey) {
        super(openaiApiKey, "gpt-4-1106-preview");
    }
}
