package lk.eternal.ai.model;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.service.AiModel;

import java.util.LinkedList;
import java.util.function.Consumer;

public interface ToolModel {

    String getName();

    void question(AiModel aiModel, LinkedList<Message> messages, Consumer<String> respConsumer);

}
