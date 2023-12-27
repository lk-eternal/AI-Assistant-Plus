package lk.eternal.ai.dto.req;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record AppReq(String action, String question, String aiModel, String toolModel, String gpt4Code) {
}