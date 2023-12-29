package lk.eternal.ai.model.plugin;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.dto.resp.ChatResp;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.model.ai.AiModel;
import lk.eternal.ai.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class BasePluginModel implements PluginModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePluginModel.class);

    private static final int MAX_HISTORY = 10;

    @Override
    public void question(AiModel aiModel, LinkedList<Message> messages, List<Plugin> plugins, Function<String, Map<String, Object>> pluginProperties, Supplier<Boolean> stopCheck, Consumer<ChatResp> respConsumer) {
        LOGGER.info("User: {}", messages.getLast().getContent());
        try {
            String resp;
            while (!stopCheck.get()) {
                final GPTResp[] respHolder = {null};

                final var prompt = getPrompt(plugins);
                while (messages.size() > MAX_HISTORY) {
                    messages.removeFirst();
                    messages.removeFirst();
                }
                aiModel.request(prompt, messages, getStops(), getTools(plugins), stopCheck, gptResp -> {
                    if (respHolder[0] == null) {
                        respHolder[0] = gptResp;
                    }
                    respHolder[0].merge(gptResp);
                    final var streamContent = gptResp.getStreamContent();
                    if (!streamContent.isBlank()) {
                        respConsumer.accept(new ChatResp(ChatResp.ChatStatus.TYPING, streamContent));
                    }
                });
                final var gptResp = respHolder[0];
                final var aiMessage = gptResp.getMessage();
                final var pluginCalls = getPluginCall(aiMessage, plugins);
                resp = gptResp.getContent();
                LOGGER.info("AI: {}", resp);
                if (pluginCalls == null || pluginCalls.isEmpty()) {
                    messages.addLast(aiMessage);
                    break;
                }
                messages.addLast(Message.create(aiModel.getModelRole(), aiMessage.getContent(), aiMessage.getTool_calls()));
                for (PluginCall pluginCall : pluginCalls) {
                    final var id = pluginCall.id();
                    final var name = pluginCall.name();
                    final var args = pluginProperties.apply(name);
                    args.putAll(pluginCall.args());
                    respConsumer.accept(new ChatResp(ChatResp.ChatStatus.FUNCTION_CALLING, name));
                    final var s = plugins.stream()
                            .filter(p -> p.name().equals(name))
                            .findFirst()
                            .map(p -> executePlugin(p, args))
                            .orElse("Unsupported plugin");
                    if (id != null) {
                        messages.add(Message.create("tool", id, name, s));
                    } else {
                        messages.add(Message.create(aiModel.getToolRole(), s, true));
                    }
                }
            }
            messages.removeIf(m -> Boolean.TRUE.equals(m.getThink()));
        } catch (Exception e) {
            LOGGER.error("Error: {}", e.getMessage(), e);
            messages.removeLast();
            respConsumer.accept(new ChatResp(ChatResp.ChatStatus.ERROR, e.getMessage()));
        }
    }

    private String executePlugin(Plugin p, Map<String, Object> args) {
        LOGGER.info("System: 执行{}命令:{}", p.name(), args);
        try {
            final var result = p.execute(args);
            LOGGER.info("System: {}{}", result.substring(0, Math.min(result.length(), 100)).replaceAll("\n", ""), result.length() > 100 ? "..." : "");
            return result;
        } catch (Exception e) {
            return "执行异常:" + e.getMessage();
        }
    }

    public abstract String getPrompt(List<Plugin> plugins);

    protected abstract List<String> getStops();

    protected abstract List<Tool> getTools(List<Plugin> plugins);

    protected abstract List<PluginCall> getPluginCall(Message message, List<Plugin> plugins);

    public record PluginCall(String id, String name, Map<String, Object> args) {

    }
}
