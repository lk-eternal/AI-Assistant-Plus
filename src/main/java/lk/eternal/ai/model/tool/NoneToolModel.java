package lk.eternal.ai.model.tool;

import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.resp.ChatResp;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.exception.GPTException;
import lk.eternal.ai.model.ai.AiModel;
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
    public void question(AiModel aiModel, LinkedList<Message> messages, Consumer<ChatResp> respConsumer) {
        LOGGER.info("User: {}", messages.getLast().getContent());
        while (messages.size() > MAX_HISTORY) {
            messages.removeFirst();
            messages.removeFirst();
        }
        try {
            final GPTResp[] respHolder = {null};
            aiModel.request(null, messages, null, null, resp -> {
                if (respHolder[0] == null) {
                    respHolder[0] = resp;
                }
                respHolder[0].merge(resp);
                final var streamContent = resp.getStreamContent();
                respConsumer.accept(new ChatResp(ChatResp.ChatStatus.TYPING, streamContent));
            });
            final var gptResp = respHolder[0];
            LOGGER.info("AI: {}", gptResp.getMessage().getContent());
            messages.addLast(gptResp.getMessage());
        } catch (GPTException e) {
            LOGGER.info("AI: {}", e.getMessage());
            respConsumer.accept(new ChatResp(ChatResp.ChatStatus.ERROR, e.getMessage()));
            messages.removeLast();
        }
    }

}
