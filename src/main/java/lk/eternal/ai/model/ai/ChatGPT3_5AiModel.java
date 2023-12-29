package lk.eternal.ai.model.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatGPT3_5AiModel extends ChatGPTAiModel {

    public ChatGPT3_5AiModel(@Value("${openai.url}") String openaiApiUrl
            , @Value("${openai.key}") String openaiApiKey) {
        super(openaiApiKey, openaiApiUrl, "gpt3.5", "gpt-3.5-turbo-1106");
    }
}
