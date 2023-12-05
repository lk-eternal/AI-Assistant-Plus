package lk.eternal.ai.model;


import lk.eternal.ai.dto.req.Message;

import java.util.LinkedList;

public interface Model {

    String getName();

    String question(LinkedList<Message> messages);

}
