package lk.eternal.ai.plugin;


import lk.eternal.ai.dto.req.Parameters;

import java.util.List;
import java.util.Map;

public interface Plugin {

    String name();

    String prompt();

    String description();

    default List<Prop> properties(){
        return List.of();
    }

    record Prop(String key, String description){

    }

    Parameters parameters();

    String execute(Map<String, Object> args);
}
