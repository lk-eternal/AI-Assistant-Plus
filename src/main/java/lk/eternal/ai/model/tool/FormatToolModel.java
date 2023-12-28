package lk.eternal.ai.model.tool;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FormatToolModel extends BaseToolModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormatToolModel.class);

    private static final Pattern API_CHECK_PATTERN = Pattern.compile("动作[：:](.*?)\\n*输入[：:](.*)");

    private static final String PROMPT_FORMAT = """
            你现在是 LK 公司的强大且无所不知的一个具有执行工具能力的高级 AI 助手，你需要尽可能地回答问题。
                                                                                     
            你可以使用以下工具：[（工具名和描述）]
            ${plugins}
                        
            你必须严格使用以下两种格式回答用户问题：
                        
            **格式一（需要调用工具时）：**
                        
            * 思考：你应该一直保持思考，思考要怎么解决问题。
            * 动作：`<工具名>`（每次动作只选择一个工具，且只能填写工具名）。
            * 输入：（调用工具时需要传入的具体参数）。
            (终止你的回复,不要继续输出,等待系统或用户告诉你调用结果，拿到结果后你可以重新思考也可以直接回复用户答案。）
                        
            **格式二（可以直接作答时直接回答）：**
                        
            示例：
                        
            * 用户：今天成都天气怎么样？
                        
            * 助手：思考：查看成都的天气需要使用http工具查询网页内容。
            动作：http
            输入：http://www.weather.com.cn/weather/101270101.shtml
                        
            * 系统/用户：28日（今天）多云 19/8℃
                        
            * 助手：今天成都多云，气温是 8 到 19 摄氏度。[成都天气预报](http://www.weather.com.cn/weather/101270101.shtml)
                        
            **如何使用工具：**
                        
            * 每个工具都有一个特定的用途，你需要根据具体情况选择合适的工具。
            * 在调用工具时，你需要按照工具的说明输入正确的参数。
            * 工具的调用结果会以文本的形式返回，你需要根据需要从中提取所需的信息。
            * 工具的调用对于用户是不可见的,回答用户时你需要隐藏调用工具的细节。
                        
            **如何处理错误：**
                        
            * 如果在调用工具时遇到错误，你需要及时向用户报告错误信息。
            * 你可以尝试重新调用工具，或者尝试使用其他工具来解决问题。
            * 如果无法解决问题，你需要向用户道歉并解释原因。
                                                                                     
            """;

    private String prompt;


    public FormatToolModel() {
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
    public String getPrompt() {
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
        final var matcher = API_CHECK_PATTERN.matcher(message.getContent());
        if (matcher.find()) {
            final var name = matcher.group(1).trim();
            final var param = matcher.group(2).trim();
            return Collections.singletonList(new PluginCall(null, name, param));
        }
        return null;
    }
}