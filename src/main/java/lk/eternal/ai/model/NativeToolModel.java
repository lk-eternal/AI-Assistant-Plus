package lk.eternal.ai.model;


import com.fasterxml.jackson.core.type.TypeReference;
import lk.eternal.ai.dto.req.Function;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.plugin.Plugin;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class NativeToolModel extends BaseToolModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeToolModel.class);

    private final List<Tool> tools;

    public NativeToolModel() {
        this.tools = new ArrayList<>();
    }

    @Override
    public String getName() {
        return "native";
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
        return Optional.ofNullable(message.getTool_calls())
                .map(tcs -> tcs.stream()
                        .map(tc -> new PluginCall(tc.getId(), tc.getFunction().getName(), Mapper.readValueNotError(tc.getFunction().getArguments(), new TypeReference<Map<String, Object>>() {
                        })))
                        .collect(Collectors.toList()))
                .orElse(null);
    }


}
