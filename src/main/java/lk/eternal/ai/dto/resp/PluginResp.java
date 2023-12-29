package lk.eternal.ai.dto.resp;

import lk.eternal.ai.plugin.Plugin;

import java.util.List;

public record PluginResp(String name, String description, List<Plugin.Prop> requireProperties) {

}
