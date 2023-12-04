package lk.eternal.ai.model;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.plugin.Plugin;
import lk.eternal.ai.service.GPTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class PluginModel implements Model {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginModel.class);


    private static final String PROMPT = """
            你现在是一个具有执行工具能力的高级AI助手,你需要尽可能的去回答问题
            你可以使用以下的工具:[(工具名和描述}]
            ${plugins}
                        
            """;

    private static final int MAX_HISTORY = 10;

    protected final GPTService gptService;
    protected Message promptMessage;

    private final Map<String, Plugin> pluginMap;

    public PluginModel(GPTService gptService) {
        this.pluginMap = new HashMap<>();
        this.gptService = gptService;
    }

    public void addPlugin(Plugin plugin) {
        pluginMap.put(plugin.name(), plugin);
        promptMessage = Message.system(PROMPT.replace("${plugins}", getPluginDescriptions()) + getPrompt(), false);
    }

    abstract protected String getPrompt();

    private String getPluginDescriptions() {
        return pluginMap.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue().description())
                .collect(Collectors.joining("\n"));
    }

    protected String request(LinkedList<Message> messages) {
        while (messages.size() > MAX_HISTORY || messages.stream().mapToInt(m -> m.content().length()).sum() > 128000 + this.promptMessage.content().length()) {
            messages.removeFirst();
        }
        final var requestMessages = new LinkedList<>(messages);
        requestMessages.addFirst(this.promptMessage);
        return this.gptService.request(requestMessages);
    }

    protected String executePlugin(String pluginName, String param) {
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
