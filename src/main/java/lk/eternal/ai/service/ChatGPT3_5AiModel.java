package lk.eternal.ai.service;

public class ChatGPT3_5AiModel extends ChatGPTAiModel {

    public ChatGPT3_5AiModel(String openaiApiKey, String openaiApiUrl) {
        super(openaiApiKey, openaiApiUrl, "gpt-3.5-turbo-1106");
    }

    @Override
    public String getName() {
        return "gpt3.5";
    }
}