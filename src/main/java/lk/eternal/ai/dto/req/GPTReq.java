package lk.eternal.ai.dto.req;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record GPTReq(String model, List<Message> messages, List<String> stop, List<Tool> tools) {

    public GPTReq(String model, List<Message> messages) {
        this(model, messages, null, null);
    }

    public GPTReq(String model, List<Message> messages, List<String> stop) {
        this(model, messages, stop, null);
    }

    public GPTReq(String model, List<Message> messages, List<String> stop, List<Tool> tools) {
        this.model = model;
        this.messages = messages;
        this.stop = stop;
        this.tools = tools;
    }
}