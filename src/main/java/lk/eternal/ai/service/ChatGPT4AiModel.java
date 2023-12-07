package lk.eternal.ai.service;

public class ChatGPT4AiModel extends ChatGPTAiModel {

    public ChatGPT4AiModel(String openaiApiKey, String openaiApiUrl) {
        super(openaiApiKey, openaiApiUrl, "gpt-4-1106-preview");
    }

    @Override
    public String getName() {
        return "gpt4";
    }
}
