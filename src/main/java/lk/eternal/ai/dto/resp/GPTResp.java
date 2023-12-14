package lk.eternal.ai.dto.resp;

import lk.eternal.ai.dto.req.Message;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public record GPTResp(String id, String object, long created, String model, List<Choice> choices, Usage usage,
                      String system_fingerprint, Error error) {

    public Message getMessage() {
        return Optional.ofNullable(choices)
                .filter(Predicate.not(List::isEmpty))
                .map(cs -> cs.get(0))
                .map(GPTResp.Choice::getMessage)
                .orElse(null);
    }

    public String getContent() {
        return Optional.ofNullable(choices)
                .filter(Predicate.not(List::isEmpty))
                .map(cs -> cs.get(0))
                .map(GPTResp.Choice::getMessage)
                .map(Message::getContent)
                .orElse("");
    }

    public String getStreamContent() {
        return Optional.ofNullable(choices)
                .filter(Predicate.not(List::isEmpty))
                .map(cs -> cs.get(0))
                .map(GPTResp.Choice::getDelta)
                .map(Message::getContent)
                .orElse("");
    }

    public void merge(GPTResp gptResp) {
        if (choices.get(0).message == null) {
            choices.get(0).message = gptResp.choices().get(0).getDelta();
        } else {
            choices.get(0).message.setContent(Optional.ofNullable(choices.get(0).message.getContent()).orElse("")
                    + Optional.ofNullable(gptResp.choices().get(0).getDelta().getContent()).orElse(""));

            final var thisToolCalls = choices.get(0).message.getTool_calls();
            final var toolCalls = gptResp.choices().get(0).getDelta().getTool_calls();
            if (toolCalls != null && !toolCalls.isEmpty()) {
                for (int i = 0; i < toolCalls.size(); i++) {
                    final var oldArguments = Optional.ofNullable(thisToolCalls.get(i).getFunction().getArguments()).orElse("");
                    final var appendArguments = Optional.ofNullable(toolCalls.get(i).getFunction().getArguments()).orElse("");
                    thisToolCalls.get(i).getFunction().setArguments(oldArguments + appendArguments);
                }
            }
        }
    }

    public static final class Choice {
        private int index;
        private Message message;
        private Message delta;
        private String finish_reason;

        public Choice() {
        }

        public Choice(int index, Message message, Message delta, String finish_reason) {
            this.index = index;
            this.message = message;
            this.delta = delta;
            this.finish_reason = finish_reason;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public Message getDelta() {
            return delta;
        }

        public void setDelta(Message delta) {
            this.delta = delta;
        }

        public String getFinish_reason() {
            return finish_reason;
        }

        public void setFinish_reason(String finish_reason) {
            this.finish_reason = finish_reason;
        }
    }

    public static final class ToolCall {
        private String id;
        private String type;
        private Function function;

        public ToolCall() {
        }

        public ToolCall(String id, String type, Function function) {
            this.id = id;
            this.type = type;
            this.function = function;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Function getFunction() {
            return function;
        }

        public void setFunction(Function function) {
            this.function = function;
        }
    }

    public static final class Function {
        private String name;
        private String arguments;

        public Function() {
        }

        public Function(String name, String arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }

    public record Usage(int prompt_tokens, int completion_tokens, int total_tokens) {
    }

    public record Error(String message, String type, String param, String code) {
    }
}

