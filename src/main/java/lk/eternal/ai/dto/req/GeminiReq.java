package lk.eternal.ai.dto.req;


import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.Collections;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record GeminiReq(List<Content> contents) {

    public record Content(String role, List<Part> parts) {

        public static Content create(String role, String content) {
            return new Content(role, Collections.singletonList(new Part(content)));
        }

        public static Content system(String content) {
            return new Content("system", Collections.singletonList(new Part(content)));
        }

        public static Content assistant(String content) {
            return new Content("assistant", Collections.singletonList(new Part(content)));
        }

        public static Content user(String content) {
            return new Content("user", Collections.singletonList(new Part(content)));
        }
    }

    public record Part(String text) {
    }
}
