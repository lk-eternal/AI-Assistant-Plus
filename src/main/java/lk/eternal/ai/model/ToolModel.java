package lk.eternal.ai.model;


import com.fasterxml.jackson.core.type.TypeReference;
import lk.eternal.ai.dto.req.Function;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.dto.resp.GPTResp;
import lk.eternal.ai.exception.GPTException;
import lk.eternal.ai.plugin.Plugin;
import lk.eternal.ai.service.GPTService;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ToolModel implements Model {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolModel.class);

    private static final int MAX_HISTORY = 10;

    protected final GPTService gptService;

    private final Map<String, Plugin> pluginMap;

    private final List<Tool> tools;

    public ToolModel(GPTService gptService) {
        this.gptService = gptService;
        this.pluginMap = new HashMap<>();
        this.tools = new ArrayList<>();
    }

    public void addTool(Plugin plugin) {
        this.pluginMap.put(plugin.name(), plugin);
        this.tools.add(new Tool(new Function(plugin.name(), plugin.description(), plugin.parameters())));
    }

    @Override
    public String getName() {
        return "tool";
    }

    @Override
    public String question(LinkedList<Message> messages) {
        LOGGER.info("User: {}", messages.getLast().content());
        String resp;
        try {
            GPTResp answer = request(messages);
            while (true) {
                final var aiMessage = answer.getMessage();
                if (!answer.isToolCall()) {
                    resp = answer.getContent();
                    LOGGER.info("AI: {}", resp);
                    messages.addLast(aiMessage);
                    break;
                }
                messages.addLast(Message.assistant(aiMessage.content(), aiMessage.tool_calls()));
                final var toolCalls = answer.getToolCalls();
                for (GPTResp.ToolCall toolCall : toolCalls) {
                    final var id = toolCall.id();
                    final var function = toolCall.function();
                    final var name = function.name();
                    final var args = Mapper.readValueNotError(function.arguments(), new TypeReference<Map<String, Object>>() {
                    });
                    final var s = executePlugin(name, args);
                    messages.add(Message.tool(id, name, s));
                }
                answer = request(messages);
            }
        } catch (Exception e) {
            messages.removeLast();
            return e.getMessage();
        }

        messages.removeIf(m -> Boolean.TRUE.equals(m.think()));
        messages.addLast(Message.assistant(resp, false));
        return resp;
    }

    private GPTResp request(LinkedList<Message> messages) throws GPTException {
        while (messages.size() > MAX_HISTORY) {
            messages.removeFirst();
        }
        final var requestMessages = new LinkedList<>(messages);
        return this.gptService.request(requestMessages, null, this.tools);
    }

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
        LOGGER.info("System: {}", result);
        return result;
    }


}
