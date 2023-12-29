package lk.eternal.ai.model.plugin;


import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.dto.resp.ChatResp;
import lk.eternal.ai.model.ai.AiModel;
import lk.eternal.ai.plugin.Plugin;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface PluginModel {

    String getName();

    void question(AiModel aiModel, LinkedList<Message> messages, List<Plugin> plugins, Function<String, Map<String, Object>> pluginProperties, Supplier<Boolean> stopCheck, Consumer<ChatResp> respConsumer);

}
