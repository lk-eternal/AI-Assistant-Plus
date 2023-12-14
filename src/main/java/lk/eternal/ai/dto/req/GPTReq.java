package lk.eternal.ai.dto.req;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record GPTReq(String model, List<Message> messages, List<String> stop, List<Tool> tools, Boolean stream) {

    public GPTReq(String model, List<Message> messages) {
        this(model, messages, null, null, null);
    }

}