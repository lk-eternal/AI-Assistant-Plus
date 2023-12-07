package lk.eternal.ai.model;

import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.exception.GPTException;
import lk.eternal.ai.service.AiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class NoneToolModel implements ToolModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoneToolModel.class);

    private static final int MAX_HISTORY = 10;

    public NoneToolModel() {
    }

    @Override
    public String getName() {
        return "none";
    }

    @Override
    public String question(AiModel aiModel, LinkedList<Message> messages) {
        LOGGER.info("User: {}", messages.getLast().content());
        var answer = request(aiModel, messages);
        LOGGER.info("AI: {}", answer);
        messages.addLast(Message.assistant(answer, false));
        return answer;
    }

    protected String request(AiModel aiModel, LinkedList<Message> messages) {
        while (messages.size() > MAX_HISTORY) {
            messages.removeFirst();
        }
        try {
            return aiModel.request(messages).getContent();
        } catch (GPTException e) {
            return e.getMessage();
        }
    }

}
