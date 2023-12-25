package lk.eternal.ai.model.tool;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.dto.resp.ChatResp;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.exception.GPTException;
import lk.eternal.ai.plugin.Plugin;
import lk.eternal.ai.model.ai.AiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BaseToolModel implements ToolModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseToolModel.class);

    private static final int MAX_HISTORY = 10;


    protected final Map<String, Plugin> pluginMap;

    public BaseToolModel() {
        this.pluginMap = new HashMap<>();
    }

    public void addPlugin(Plugin plugin) {
        this.pluginMap.put(plugin.name(), plugin);
    }

    @Override
    public void question(AiModel aiModel, LinkedList<Message> messages, Consumer<ChatResp> respConsumer) {
        LOGGER.info("User: {}", messages.getLast().getContent());
        try {
            String resp;
            while (true) {
                final GPTResp[] respHolder = {null};
                request(aiModel, messages, getStops(), getTools(), gptResp -> {
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
                final var pluginCalls = getPluginCall(aiMessage);
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
                    final var args = pluginCall.args();
                    respConsumer.accept(new ChatResp(ChatResp.ChatStatus.FUNCTION_CALLING, name));
                    final var s = executePlugin(name, args);
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

    protected void request(AiModel aiModel, LinkedList<Message> messages, List<String> stops, List<Tool> tools, Consumer<GPTResp> respConsumer) throws GPTException {
        final var prompt = getPrompt();
        while (messages.size() > MAX_HISTORY) {
            messages.removeFirst();
            messages.removeFirst();
        }
        aiModel.request(prompt, messages, stops, tools, respConsumer);
    }

    protected abstract String getPrompt();

    protected abstract List<String> getStops();

    protected abstract List<Tool> getTools();

    protected abstract List<PluginCall> getPluginCall(Message message);

    protected String executePlugin(String pluginName, Object param) {
        if (!this.pluginMap.containsKey(pluginName)) {
            return "不支持该工具";
        }
        LOGGER.info("System: 执行{}命令:{}", pluginName, param);
        final String result;
        try {
            result = this.pluginMap.get(pluginName).execute(param);
        } catch (Exception e) {
            return "执行异常:" + e.getMessage();
        }
        LOGGER.info("System: {}{}", result.substring(0, Math.min(result.length(), 100)).replaceAll("\n", ""), result.length() > 100 ? "..." : "");
        return result;
    }

    public record PluginCall(String id, String name, Object args) {

    }
}
