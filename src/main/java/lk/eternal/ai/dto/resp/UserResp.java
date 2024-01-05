package lk.eternal.ai.dto.resp;


import lk.eternal.ai.domain.User;
import lk.eternal.ai.dto.req.Message;

import java.util.List;
import java.util.Map;

public class UserResp {

    private final boolean gpt4Enable;

    private final List<String> messages;

    private final Map<String, Object> properties;

    public UserResp(User user) {
        this.gpt4Enable = user.isGpt4Enable();
        this.messages = user.getMessages().stream().map(Message::getContent).toList();
        this.properties = user.getProperties();
    }

    public boolean isGpt4Enable() {
        return gpt4Enable;
    }

    public List<String> getMessages() {
        return messages;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
