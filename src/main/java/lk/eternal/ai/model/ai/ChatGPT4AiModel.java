package lk.eternal.ai.model.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatGPT4AiModel extends ChatGPTAiModel {

    public ChatGPT4AiModel(@Value("${openai.url}") String openaiApiUrl
            , @Value("${openai.key}") String openaiApiKey) {
        super(openaiApiKey, openaiApiUrl, "gpt4", "gpt-4-1106-preview");
    }
}
