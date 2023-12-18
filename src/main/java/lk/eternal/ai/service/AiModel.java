package lk.eternal.ai.service;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.exception.GPTException;

import java.util.List;
import java.util.function.Consumer;

public interface AiModel {

    String getName();

    void request(List<Message> messages, List<String> stop, List<Tool> tools, Consumer<GPTResp> respConsumer) throws GPTException;
}
