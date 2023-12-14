package lk.eternal.ai.model;

import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.exception.GPTException;
import lk.eternal.ai.service.AiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.function.Consumer;

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
    public void question(AiModel aiModel, LinkedList<Message> messages, Consumer<String> respConsumer) {
        LOGGER.info("User: {}", messages.getLast().getContent());
        while (messages.size() > MAX_HISTORY) {
            messages.removeFirst();
        }
        StringBuilder sb = new StringBuilder();
        try {
            aiModel.request(messages, null, null, resp -> {
                final var streamContent = resp.getStreamContent();
                sb.append(streamContent);
                respConsumer.accept(streamContent);
            });
            LOGGER.info("AI: {}", sb);
            messages.addLast(Message.assistant(sb.toString(), false));
        } catch (GPTException e) {
            LOGGER.info("AI: {}", e.getMessage());
            respConsumer.accept(e.getMessage());
            messages.removeLast();
        }
    }

}
