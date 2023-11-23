package lk.eternal.ai.model;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.service.ChatGPT4Service;
import lk.eternal.ai.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PromptModel implements Model {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptModel.class);

    private static final Pattern API_CHECK_PATTERN = Pattern.compile("动作[：:](.*?)\\n*输入[：:](.*)");
    private static final Pattern ANSWER_CHECK_PATTERN = Pattern.compile("最终结果[：:](.*)", Pattern.DOTALL);

    private static final int MAX_HISTORY = 10;

    private static final String PROMPT = """
            你需要尽可能的去回答问题
            你可以使用以下的工具:[(工具名和描述}]
            ${tools}
                        
            必须严格使用以下格式回答:
                        
            问题: 你必须回答的问题
                        
            思考:你应该一直保持思考，思考要怎么解决问题
            动作:<工具名>,每次动作只选择一个工具,且只能填写工具名
            输入:<调用工具时需要传入的参数>.
            ...(然后等待系统的回复)
            观察:<第三方工具返回的结果>
                        
            ...(这个“思考/动作/输入/观察”的过程可以重复N次)
                        
            最终结果:针对于原始问题，输出最终结果，如果有引用来源需要加上引用地址
                        
            开始!
                        
            """;

    private static final LinkedList<Message> messages = new LinkedList<>();

    private final ChatGPT4Service chatGPT4Service;
    private final Map<String, Service> serviceMap;

    public PromptModel(ChatGPT4Service chatGPT4Service) {
        this.serviceMap = new HashMap<>();
        this.chatGPT4Service = chatGPT4Service;
    }

    @Override
    public void addService(Service service) {
        serviceMap.put(service.name(), service);
    }

    @Override
    public String question(String question) {
        LOGGER.info("User: {}", question);
        messages.addLast(Message.user(question));
        var answer = request();
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
                final var content = Optional.ofNullable(executeCmd(cmd, param)).filter(Predicate.not(String::isBlank)).orElse("无数据");
                messages.addLast(Message.assistant(answer, true));
                messages.addLast(Message.system(content, true));
                answer = request();
            } else {
                break;
            }
        }
        messages.removeIf(Message::think);
        messages.addLast(Message.assistant(answer, false));
        return answer;
    }

    private String request() {
        final var prompt = getPrompt();
        while (messages.size() > MAX_HISTORY || messages.stream().mapToInt(m -> m.content().length()).sum() > 128000 + prompt.content().length()) {
            messages.removeFirst();
        }
        final var requestMessages = new LinkedList<>(messages);
        requestMessages.addFirst(prompt);
        return this.chatGPT4Service.request(requestMessages);
    }

    private Message getPrompt() {
        final var tools = serviceMap.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue().description())
                .collect(Collectors.joining("\n"));
        return Message.system(PROMPT.replace("${tools}", tools), false);
    }

    private String executeCmd(String cmd, String param) {
        if (!this.serviceMap.containsKey(cmd)) {
            return "不支持该工具";
        }
        LOGGER.info("System: 执行{}命令:{}", cmd, param);
        final String result;
        try {
            result = this.serviceMap.get(cmd).execute(param);
        } catch (Exception e) {
            return "执行异常:" + e.getMessage();
        }
        LOGGER.info("System: {}", result);
        return result;
    }
}