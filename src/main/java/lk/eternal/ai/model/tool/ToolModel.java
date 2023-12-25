package lk.eternal.ai.model.tool;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.resp.ChatResp;
import lk.eternal.ai.model.ai.AiModel;

import java.util.LinkedList;
import java.util.function.Consumer;

public interface ToolModel {

    String getName();

    void question(AiModel aiModel, LinkedList<Message> messages, Consumer<ChatResp> respConsumer);

}
