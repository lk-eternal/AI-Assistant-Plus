package lk.eternal.ai.model;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.exception.GPTException;
import lk.eternal.ai.plugin.Plugin;
import lk.eternal.ai.service.AiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    public String question(AiModel aiModel, LinkedList<Message> messages) {
        LOGGER.info("User: {}", messages.getLast().content());
        String resp;
        try {
            GPTResp answer = request(aiModel, messages, getStops(), getTools());
            while (true) {
                final var aiMessage = answer.getMessage();
                final var pluginCalls = getPluginCall(aiMessage);
                if (pluginCalls == null || pluginCalls.isEmpty()) {
                    resp = answer.getContent();
                    LOGGER.info("AI: {}", resp);
                    messages.addLast(aiMessage);
                    break;
                }
                messages.addLast(Message.assistant(aiMessage.content(), aiMessage.tool_calls()));
                for (PluginCall pluginCall : pluginCalls) {
                    final var id = pluginCall.id();
                    final var name = pluginCall.name();
                    final var args = pluginCall.args();
                    final var s = executePlugin(name, args);
                    messages.add(Message.tool(id, name, s));
                }
                answer = request(aiModel, messages, getStops(), getTools());
            }
        } catch (Exception e) {
            messages.removeLast();
            return e.getMessage();
        }

        messages.removeIf(m -> Boolean.TRUE.equals(m.think()));
        messages.addLast(Message.assistant(resp, false));
        return resp;
    }

    protected GPTResp request(AiModel aiModel, LinkedList<Message> messages, List<String> stops, List<Tool> tools) throws GPTException {
        final var prompt = getPrompt();
        while (messages.size() > MAX_HISTORY) {
            messages.removeFirst();
        }
        final var requestMessages = new LinkedList<>(messages);
        if (prompt != null) {
            requestMessages.addFirst(Message.system(prompt, false));
        }
        return aiModel.request(requestMessages, stops, tools);
    }

    protected abstract String getPrompt();

    protected abstract List<String> getStops();

    protected abstract List<Tool> getTools();

    protected abstract List<PluginCall> getPluginCall(Message message);

    protected String executePlugin(String pluginName, Map<String, Object> param) {
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

    public record PluginCall(String id, String name, Map<String, Object> args) {

    }
}
