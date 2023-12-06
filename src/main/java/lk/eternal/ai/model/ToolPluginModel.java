package lk.eternal.ai.model;


import com.fasterxml.jackson.core.type.TypeReference;
import lk.eternal.ai.dto.req.Function;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.plugin.Plugin;
import lk.eternal.ai.service.GPTService;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ToolPluginModel extends PluginModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolPluginModel.class);

    private final List<Tool> tools;

    public ToolPluginModel(GPTService gptService) {
        super(gptService);
        this.tools = new ArrayList<>();
    }

    @Override
    public String getName() {
        return "tool";
    }

    @Override
    public void addPlugin(Plugin plugin) {
        super.addPlugin(plugin);
        this.tools.add(new Tool(new Function(plugin.name(), plugin.description(), plugin.parameters())));
    }

    @Override
    protected String getPrompt() {
        return null;
    }

    @Override
    protected List<String> getStops() {
        return null;
    }

    @Override
    protected List<Tool> getTools() {
        return this.tools;
    }

    @Override
    protected List<PluginCall> getPluginCall(Message message) {
        return Optional.ofNullable(message.tool_calls())
                .map(tcs -> tcs.stream()
                        .map(tc -> new PluginCall(tc.id(), tc.function().name(), Mapper.readValueNotError(tc.function().arguments(), new TypeReference<>() {
                        })))
                        .collect(Collectors.toList()))
                .orElse(null);
    }


}
