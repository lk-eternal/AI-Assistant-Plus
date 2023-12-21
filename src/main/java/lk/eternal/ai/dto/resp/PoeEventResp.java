package lk.eternal.ai.dto.resp;


public record PoeEventResp(String event, Object data) {

    public record TextEvent(String text) {
    }

    public record ReplaceResponseEvent(String text) {
    }

    /**
     * @param error_type user_message_too_long
     */
    public record ErrorEvent(boolean allow_retry, String text, String error_type) {
    }

    public record DoneEvent() {
    }

}
