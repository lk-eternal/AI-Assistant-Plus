package lk.eternal.ai.dto.resp;


public record ChatResp(ChatStatus status, String message) {
    public enum ChatStatus{
        TYPING,
        FUNCTION_CALLING,
        ERROR,
    }
}
