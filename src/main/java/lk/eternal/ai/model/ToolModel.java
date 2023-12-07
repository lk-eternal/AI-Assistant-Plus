package lk.eternal.ai.model;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.service.AiModel;

import java.util.LinkedList;

public interface ToolModel {

    String getName();

    String question(AiModel aiModel, LinkedList<Message> messages);

}
