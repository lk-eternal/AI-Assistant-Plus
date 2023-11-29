package lk.eternal.ai.model;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.service.GPTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class PromptModel extends PluginModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptModel.class);

    private static final Pattern API_CHECK_PATTERN = Pattern.compile("动作[：:](.*?)\\n*输入[：:](.*)");
    private static final Pattern ANSWER_CHECK_PATTERN = Pattern.compile("最终结果[：:](.*)", Pattern.DOTALL);

    private static final String PROMPT = """
            你必须严格使用以下格式回答用户问题(括号中的内容是注意事项,不需要打印出来):
            思考:你应该一直保持思考,思考要怎么解决问题
            动作:<工具名>(每次动作只选择一个工具,且只能填写工具名)
            输入:(调用工具时需要传入的具体参数)
            (停止回复)
            (然后等待系统给出工具的响应结果)
            (系统响应后可以重复这个“思考/动作/输入/等待响应结果”的过程,或者给出最终结果)
                        
            最终结果:针对于原始问题,输出最终结果,如果有引用来源需要加上引用地址
                        
            示例:
            用户:今天成都天气怎么样?
            助手:思考: 查看成都的天气需要使用web工具查询网页内容.
            动作: web
            输入: http://www.weather.com.cn/weather/101270101.shtml
            系统:28日（今天） 多云 19/8℃
            助手:最终结果:今天成都多云,气温是8到19摄氏度.
            """;

    public PromptModel(GPTService gptService) {
        super(gptService);
    }

    protected String getPrompt() {
        return PROMPT;
    }

    @Override
    public String question(LinkedList<Message> messages) {
        LOGGER.info("User: {}", messages.getLast().content());
        var answer = request(messages);
        while (true) {
            LOGGER.info("AI: {}", answer);
            final var answerMatcher = ANSWER_CHECK_PATTERN.matcher(answer);
            if (answerMatcher.find()) {
                answer = answerMatcher.group(1).trim();
                break;
            }
            final var matcher = API_CHECK_PATTERN.matcher(answer);
            if (matcher.find()) {
                final var cmd = matcher.group(1).trim();
                final var param = matcher.group(2).trim();
                final var content = Optional.ofNullable(executePlugin(cmd, param)).filter(Predicate.not(String::isBlank)).orElse("无数据");
                messages.addLast(Message.assistant(answer, true));
                messages.addLast(Message.system(content, true));
                answer = request(messages);
            } else {
                break;
            }
        }
        messages.removeIf(Message::think);
        messages.addLast(Message.assistant(answer, false));
        return answer;
    }
}