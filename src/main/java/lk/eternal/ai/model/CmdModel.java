package lk.eternal.ai.model;

import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.service.GPTService;
import lk.eternal.ai.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CmdModel implements Model{

    private static final Logger LOGGER = LoggerFactory.getLogger(CmdModel.class);

    private static final Pattern API_CHECK_PATTERN = Pattern.compile("(?s)(.*?)\n*\\[api:(.*?)\\](.*?)\\[api\\]\n*(.*?)");

    private static final int MAX_HISTORY = 10;
    private static final String PROMPT = """
            你现在是一个高级AI助手,可以帮助用户完成需求.
            为了更加完美的满足用户需求,你被赋予调用外部命令的能力,在你认为有必要时可以调用外部命令获取信息,调用方式为在消息中包含[api:xx]调用内容[api]
            调用的部分的消息用户无法看到,外部响应将由system身份回复你,外部响应的内容用户也无法看到.
                        
            在帮助用户完成需求的过程中需要遵守以下规则:
            1. 允许AI在必要时自主调用外部API以获取或验证所需信息。例如：天气查询、数据库操作、网络查找等。
            2. 鼓励AI在已有上下文信息的基础上进行逻辑推断，且无需获取额外用户输入时即可执行任务。比如根据之前的交互记录，AI能够推断出需要执行的数据库查询或Web搜索。
            3. AI需要理解并熟悉数据库架构及网络信息结构，以自动构建查询或搜索请求，获取所需的数据及信息。
            4. 当遇到模糊或不完整信息时，AI能够基于最可能的假设自动进行决策，并执行相应的操作，而不是返回给用户请求更多的指导。
            5. 当出现错误或无法执行的操作时，AI应能够自行解决问题或提出替代方案，而不是简单地返回错误信息。
            6. 将AI的反馈信息设计得简洁明了，便于用户理解操作结果，避免过多的技术细节。
            
            请注意:
            1.你无法主动给系统发送消息.
            2.系统可以赋予你新的能力(扩展命令),系统赋予的能力,除了额外说明外新能力的规则和当前这个规则保持一致.
            
            初始能力(命令)有:
            1.[api:sql],你被赋予执行sql的能力,参数是sql语句,数据库是Postgresql.对于操作数据库的需求,你会先尝试检索数据库结构或表结构,从中推断所需信息,从不直接询问用户细节(表名或者字段名).
            2.[api:web],你被赋予查询网络信息的能力,命令参数是网址,该命令会返回整个网页源码,你需要从中提取需要的信息.
            3.[api:calc],你被赋予调用数学计算的能力,参数是数学表达式,该命令实际计算使用的是hutool库的Calculator类的conversion方法,你需要构造出合适的表达式.
            
            下面是一个系统扩展命令的例子:
            System: 新增一个[web]命令,用于需要进行网络查询时.
            Assistant: [Resp] 好的,我已经拥有了网络查询的能力,在必要时我会使用网络查询来为用户提供更好的答案.
            User: 今天xxx天气怎么样?
            Assistant: [api:web]http://www.weather.com.cn/weather/101270101.shtml[api]
            System: [api:resp] <html>...<html>
            Assistant: 今天xxx天气是xx.
            
            下面是一个执行sql的例子:
            User: 帮我创建一张学生表
            Assistant: 这张表有什么字段呢?
            User: 有姓名,性别,年龄,电话号码.
            Assistant: 好的,这是sql代码 代码段: CREATE TABLE student ...,确定要执行吗?
            User: 确定
            Assistant: 请稍等,这就为您创建...[api:sql]CREATE TABLE student ...[api]
            System: [api:resp] Updated Rows 0
            Query CREATE TABLE student (
            uuid UUID PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            gender CHAR(1) CHECK (gender IN ('M', 'F')),
            age INT NOT NULL,
            phone_number VARCHAR(15)
            )
            Finish time Fri Nov 17 10:28:16 CST 2023
            Assistant: 学生表已经创建好了,请问还有什么需要帮助的吗?
            """;


    private final Map<String, List<Message>> sessionMessageMap = new HashMap<>();

    private final GPTService gptService;
    private final Map<String, Service> serviceMap;

    public CmdModel(GPTService gptService) {
        this.serviceMap = new HashMap<>();
        this.gptService = gptService;
    }

    @Override
    public void addService(Service service) {
        serviceMap.put(service.name(), service);
    }

    @Override
    public String question(String sessionId, String question) {
        LOGGER.info("User: {}", question);
        final var messages = (LinkedList<Message>)sessionMessageMap.computeIfAbsent(sessionId, k -> new LinkedList<>());
        messages.addLast(Message.user(question));
        var answer = request(messages);
        while (true) {
            LOGGER.info("AI: {}", answer);
            final var matcher = API_CHECK_PATTERN.matcher(answer);
            if (matcher.find()) {
                final var cmd = matcher.group(2);
                final var param = matcher.group(3);
                final var content = Optional.ofNullable(executeCmd(cmd, param)).filter(Predicate.not(String::isBlank)).orElse("无数据");
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

    private String request(LinkedList<Message> messages) {
        final var prompt = getPrompt();
        while (messages.size() > MAX_HISTORY || messages.stream().mapToInt(m -> m.content().length()).sum() > 128000 + prompt.content().length()) {
            messages.removeFirst();
        }
        final var requestMessages = new LinkedList<>(messages);
        requestMessages.addFirst(prompt);
        return this.gptService.request(requestMessages);
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
