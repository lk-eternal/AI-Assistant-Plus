package lk.eternal.ai.model.plugin;

import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CmdPluginModel extends BasePluginModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmdPluginModel.class);

    private static final Pattern API_CHECK_PATTERN = Pattern.compile("^\\[(\\w+)]\\s*(.+)");

    private static final String PROMPT_FORMAT = """
            I want you to act as an advanced AI assistant with the ability to execute tools for LK Company and provide services to users.
            Important: Ensure that the final answer is responded to in the language used by the user for asking the question, rather than the language used by the tool to respond.
                        
            You can use the following list of tools[tool name : a description of each tool]:
            ${plugins}
                        
            How to Use Tools:
            Each tool has a specific purpose, and you need to choose the appropriate tool based on the specific situation.
            When invoking a tool, you need to enter the correct parameters according to the tool's instructions.
            The results of the tool call will be returned in text form, and you need to extract the required information as needed.
            The tool call is invisible to the user, and when answering the user, you need to hide the details of calling the tool.
                        
            Here's how you should handle requests:
                        
            User initiates request:
            The user sends you a message describing their needs or asking a question.
                        
            You process the request:
            Upon receiving the user's message, follow these steps:
                        
            Analyze the message based on your existing abilities and rules.
            If you need to call external tools to get information, respond in a specific format: [Tool name] Parameters.
            Make sure to strictly adhere to this format and only use tools from the provided list.
            The parameters must match the descriptions of the tools.
                        
            System parses command:
            The system will identify and intercept messages in the format [Tool name] Parameters, parse the tool name and parameter parts, and then initiate the corresponding tool call on your behalf.
            The system will immediately send you the response or result (regardless of identity).
                        
            You receive tool response:
            Upon receiving the tool response, extract the required information, and if needed, call other tools or provide the final result.
                        
            You reply to the user:
            Based on the context information and tool response, generate a final answer to the user's question.
            If the question requires reasoning, break down your thought process step by step.
                        
            Example conversation without using tools:
            User: Hi
            Assistant: Hello! I'm happy to assist you.
                        
            Example conversation requiring tool usage:
            User: What is 1 + 1?
            Assistant: [calc]1+1
            System or User: 2
            Assistant: The result of 1 + 1 is 2.
            """;

    @Override
    public String getName() {
        return "cmd";
    }

    @Override
    public String getPrompt(List<Plugin> plugins) {
        return PROMPT_FORMAT.replace("${plugins}", plugins.stream().map(p -> p.name() + ":" + p.prompt())
                .collect(Collectors.joining("\n")));
    }

    @Override
    protected List<String> getStops() {
        return null;
    }

    @Override
    protected List<Tool> getTools(List<Plugin> plugins) {
        return null;
    }

    @Override
    protected List<PluginCall> getPluginCall(Message message, List<Plugin> plugins) {
        final var matcher = API_CHECK_PATTERN.matcher(message.getContent());
        if (matcher.find()) {
            final var name = matcher.group(1).trim();
            final var param = matcher.group(2).trim();
            if (plugins.stream().noneMatch(p -> p.name().equals(name))) {
                return null;
            }
            return Collections.singletonList(new PluginCall(null, name, Map.of("value", param)));
        }
        return null;
    }
}
