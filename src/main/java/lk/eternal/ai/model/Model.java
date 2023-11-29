package lk.eternal.ai.model;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.plugin.Plugin;

import java.util.LinkedList;

public interface Model {

    String question(LinkedList<Message> messages);

    void addPlugin(Plugin plugin);
}
