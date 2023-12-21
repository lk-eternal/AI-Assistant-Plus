package lk.eternal.ai.dto.req;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lk.eternal.ai.dto.resp.GPTResp;

import java.util.List;
import java.util.Objects;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties("think")
public final class Message {
    private String role;
    private String content;
    private List<GPTResp.ToolCall> tool_calls;
    private String tool_call_id;
    private String name;
    private Boolean think;

    public Message() {
    }

    public Message(String role, String content, List<GPTResp.ToolCall> tool_calls, String tool_call_id, String name,
                   Boolean think) {
        this.role = role;
        this.content = content;
        this.tool_calls = tool_calls;
        this.tool_call_id = tool_call_id;
        this.name = name;
        this.think = think;
    }

    public static Message create(String role, String content, boolean isThink) {
        return new Message(role, content, null, null, null, isThink);
    }

    public static Message create(String role, String content, List<GPTResp.ToolCall> tool_calls) {
        return new Message(role, content, tool_calls, null, null, true);
    }

    public static Message create(String role, String id, String name, String content) {
        return new Message(role, content, null, id, name, true);
    }

    public static Message user(String content) {
        return new Message("user", content, null, null, null, false);
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<GPTResp.ToolCall> getTool_calls() {
        return tool_calls;
    }

    public void setTool_calls(List<GPTResp.ToolCall> tool_calls) {
        this.tool_calls = tool_calls;
    }

    public String getTool_call_id() {
        return tool_call_id;
    }

    public void setTool_call_id(String tool_call_id) {
        this.tool_call_id = tool_call_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getThink() {
        return think;
    }

    public void setThink(Boolean think) {
        this.think = think;
    }
}