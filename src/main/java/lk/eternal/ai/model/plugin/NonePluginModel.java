package lk.eternal.ai.model.plugin;

import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.resp.ChatResp;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.exception.GPTException;
import lk.eternal.ai.model.ai.AiModel;
import lk.eternal.ai.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class NonePluginModel implements PluginModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonePluginModel.class);

    private static final int MAX_HISTORY = 10;

    @Override
    public String getName() {
        return "none";
    }

    @Override
    public void question(AiModel aiModel, LinkedList<Message> messages, List<Plugin> plugins, Function<String, Map<String, Object>> pluginProperties, Supplier<Boolean> stopCheck, Consumer<ChatResp> respConsumer) {
        LOGGER.info("User: {}", messages.getLast().getContent());
        while(messages.size() > MAX_HISTORY || messages.get(0).getRole().equals(aiModel.getModelRole())){
            messages.removeFirst();
        }
        try {
            final GPTResp[] respHolder = {null};
            aiModel.request(null, messages, null, null, stopCheck, resp -> {
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
