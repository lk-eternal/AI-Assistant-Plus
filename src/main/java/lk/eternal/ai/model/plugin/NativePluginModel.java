package lk.eternal.ai.model.plugin;


import com.fasterxml.jackson.core.type.TypeReference;
import lk.eternal.ai.dto.req.Function;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.req.Tool;
import lk.eternal.ai.plugin.Plugin;
import lk.eternal.ai.util.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class NativePluginModel extends BasePluginModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativePluginModel.class);

    @Override
    public String getName() {
        return "native";
    }

    @Override
    public String getPrompt(List<Plugin> plugins) {
        return null;
    }

    @Override
    protected List<String> getStops() {
        return null;
    }

    @Override
    protected List<Tool> getTools(List<Plugin> plugins) {
        return plugins.stream().map(plugin -> new Tool(new Function(plugin.name(), plugin.description(), plugin.parameters()))).toList();
    }

    @Override
    protected List<PluginCall> getPluginCall(Message message, List<Plugin> plugins) {
        return Optional.ofNullable(message.getTool_calls())
                .map(tcs -> tcs.stream()
                        .map(tc -> new PluginCall(tc.getId(), tc.getFunction().getName(), Mapper.readValueNotError(tc.getFunction().getArguments(), new TypeReference<>() {
                        })))
                        .collect(Collectors.toList()))
                .orElse(null);
    }


}
