package lk.eternal.ai.dto.req;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties("think")
public record Message(String role, String content, boolean think) {

    public static Message system(String content, boolean isThink) {
        return new Message("system", content, isThink);
    }

    public static Message assistant(String content, boolean isThink) {
        return new Message("assistant", content, isThink);
    }

    public static Message user(String content) {
        return new Message("user", content, false);
    }

}