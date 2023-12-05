package lk.eternal.ai.dto.req;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lk.eternal.ai.dto.resp.GPTResp;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties("think")
public record Message(String role, String content, List<GPTResp.ToolCall> tool_calls, String tool_call_id, String name, Boolean think) {

    public static Message system(String content, boolean isThink) {
        return new Message("system", content, null, null, null, isThink);
    }

    public static Message assistant(String content, boolean isThink) {
        return new Message("assistant", content, null, null, null, isThink);
    }

    public static Message assistant(String content, List<GPTResp.ToolCall> tool_calls) {
        return new Message("assistant", content, tool_calls, null, null, true);
    }

    public static Message user(String content) {
        return new Message("user", content, null, null, null, false);
    }

    public static Message tool(String id, String name, String content) {
        return new Message("tool", content, null, id, name, true);
    }

}