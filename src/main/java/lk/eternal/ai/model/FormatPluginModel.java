package lk.eternal.ai.model;


import com.fasterxml.jackson.core.type.TypeReference;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.plugin.Plugin;
import lk.eternal.ai.service.GPTService;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FormatPluginModel extends PluginModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormatPluginModel.class);

    private static final Pattern API_CHECK_PATTERN = Pattern.compile("动作[：:](.*?)\\n*输入[：:](.*)");

    private static final String PROMPT_FORMAT = """
            你现在是LK公司的强大且无所不知的一个具有执行工具能力的高级AI助手,你需要尽可能的去回答问题
            你可以使用以下的工具:[(工具名和描述}]
            ${plugins}
                        
            你必须严格使用以下格式回答用户问题(括号中的内容是注意事项,不需要打印出来):
            思考:你应该一直保持思考,思考要怎么解决问题
            动作:<工具名>(每次动作只选择一个工具,且只能填写工具名)
            输入:(调用工具时需要传入的具体参数)
            (停止回复)
            (然后等待系统给出工具的响应结果)
            (系统响应后可以重复这个“思考/动作/输入/等待响应结果”的过程,或者给出最终结果)
                        
            最终结果:针对于原始问题,输出最终结果,如果有引用来源需要加上引用地址,地址前后加上空格
                        
            示例:
            用户:今天成都天气怎么样?
            助手:思考: 查看成都的天气需要使用web工具查询网页内容.
            动作: http
            输入: http://www.weather.com.cn/weather/101270101.shtml
            系统:28日（今天） 多云 19/8℃
            助手:最终结果:今天成都多云,气温是8到19摄氏度. http://www.weather.com.cn/weather/101270101.shtml\s
            """;

    private String prompt;


    public FormatPluginModel(GPTService gptService) {
        super(gptService);
    }

    @Override
    public String getName() {
        return "format";
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