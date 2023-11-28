package lk.eternal.ai.service;


import lk.eternal.ai.dto.req.Message;

import java.util.List;

public interface GPTService {

    default String request(List<Message> messages) {
        return request(messages, null);
    }

    String request(List<Message> messages, List<String> stops);
}
