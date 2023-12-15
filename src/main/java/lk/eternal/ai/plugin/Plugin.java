package lk.eternal.ai.plugin;


import lk.eternal.ai.dto.req.Parameters;

public interface Plugin {

    String name();

    String description();

    Parameters parameters();

    String execute(Object args);
}
