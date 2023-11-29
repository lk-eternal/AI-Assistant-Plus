package lk.eternal.ai.model;


import lk.eternal.ai.plugin.Plugin;

public interface Model {

    String question(String sessionId, String question);

    void addPlugin(Plugin plugin);
}
