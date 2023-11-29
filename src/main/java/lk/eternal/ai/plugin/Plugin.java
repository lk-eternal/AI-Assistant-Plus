package lk.eternal.ai.plugin;


public interface Plugin {

    String name();

    String description();

    String execute(String param);
}
