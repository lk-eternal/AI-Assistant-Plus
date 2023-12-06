package lk.eternal.ai.plugin;


import lk.eternal.ai.dto.req.Parameters;

import java.util.Map;

public interface Plugin {

    String name();

    String description();

    Parameters parameters();

    String execute(Map<String, Object> args);
}
