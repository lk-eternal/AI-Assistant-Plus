package lk.eternal.ai.dto.resp;

import lk.eternal.ai.dto.req.Message;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public record GPTResp(String id, String object, long created, String model, List<Choice> choices, Usage usage,
                      String system_fingerprint, Error error) {

    public Message getMessage() {
        return Optional.ofNullable(choices)
                .filter(Predicate.not(List::isEmpty))
                .map(cs -> cs.get(0))
                .map(GPTResp.Choice::message)
                .orElse(null);
    }

    public String getContent() {
        return Optional.ofNullable(choices)
                .filter(Predicate.not(List::isEmpty))
                .map(cs -> cs.get(0))
                .map(GPTResp.Choice::message)
                .map(Message::content)
                .orElse("");
    }

    public record Choice(int index, Message message, String finish_reason) {
    }

    public record ToolCall(String id, String type, Function function) {
    }

    public record Function(String name, String arguments) {
    }

    public record Usage(int prompt_tokens, int completion_tokens, int total_tokens) {
    }

    public record Error(String message, String type, String param, String code) {
    }
}

