package lk.eternal.ai.model;

import com.fasterxml.jackson.core.type.TypeReference;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.plugin.Plugin;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CmdToolModel extends BaseToolModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmdToolModel.class);

    private static final Pattern API_CHECK_PATTERN = Pattern.compile("\\[(\\w+)](.+?)\\[\\1]");

    private static final String PROMPT_FORMAT = """
            你现在是LK公司的强大且无所不知的一个具有执行工具能力的高级AI助手,你需要尽可能的去回答问题
            你可以使用以下的工具:[(工具名和描述}]
            ${plugins}
                        
            你需要按照下列流程来回答问题
            用户发起请求：
                用户向AI发送消息，描述他们的需求或提出问题。
            AI处理请求：
                AI接收到用户的消息，根据已有的能力和规则进行逻辑推断和处理。
                如果AI需要调用外部工具来获取信息，它会以特定的格式在消息中包含相应的命令，标记为 [工具名]参数[工具名]，你必须严格遵守这个格式，首位的标记都不能缺失。
            系统解析命令：
                系统作为接收方接收到AI发送的消息，并解析其中的命令部分。
                系统根据解析到的命令类型，执行相应的操作或调用相应的外部服务。
                系统将外部服务的响应或结果作为系统身份发送给AI。
            AI获取外部响应：
                AI作为接收方接收到系统发送的外部响应。
                AI根据需要从外部响应中提取所需的信息，获取外部响应后可以根据需要重复这个“调用外部工具/获取外部响应”的过程，或者给出最终结果。
            AI回复用户：
                AI根据处理结果和外部响应，生成最终答案的回复消息。
                AI将回复消息发送给用户，提供所需的信息或完成用户的需求。
                        
            下面是一个对话示例:
            User: 1 + 1 = ?
            Assistant: [calc]1+1[calc]
            System: 2
            Assistant: 1 + 1 = 2
            """;

    private String prompt;


    public CmdToolModel() {
    }

    @Override
    public String getName() {
        return "cmd";
    }

    @Override
    public void addPlugin(Plugin plugin) {
        super.addPlugin(plugin);
        this.prompt = PROMPT_FORMAT.replace("${plugins}", this.pluginMap.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue().description())
                .collect(Collectors.joining("\n")));
    }


    @Override
    protected String getPrompt() {
        return this.prompt;
    }

    @Override
    protected List<String> getStops() {
        return null;
    }

    @Override
    protected List<Tool> getTools() {
        return null;
    }


    @Override
    protected List<PluginCall> getPluginCall(Message message) {
        final var matcher = API_CHECK_PATTERN.matcher(message.content());
        if (matcher.find()) {
            final var name = matcher.group(1).trim();
            final var param = matcher.group(2).trim();
            return Collections.singletonList(new PluginCall(null, name, Mapper.readValueNotError(param, new TypeReference<>() {
            })));
        }
        return null;
    }
}