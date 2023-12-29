package lk.eternal.ai.domain;

import lk.eternal.ai.dto.req.Message;

import java.util.*;
import java.util.stream.Collectors;

public class User {

    private final String id;
    private final LinkedList<Message> messages;
    private final Map<String, Object> properties;
    private Status status;

    public User(String id) {
        this.id = id;
        this.messages = new LinkedList<>();
        this.properties = new HashMap<>();
        this.status = Status.WAITING;
    }

    public String getId() {
        return id;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    public LinkedList<Message> getMessages() {
        return messages;
    }

    public void putProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    public String getAiModel() {
        return getProperty("aiModel").toString();
    }

    public String getPluginModel() {
        return getProperty("pluginModel").toString();
    }

    public String getGpt4Code() {
        return getProperty("gpt4Code").toString();
    }

    @SuppressWarnings("unchecked")
    public List<String> getPlugins(){
        return (List<String>) Optional.ofNullable(getProperty("plugins"))
                .filter(ps -> ps instanceof List)
                .orElse(null);
    }

    public void clear(){
        this.messages.clear();
    }

    public Map<String, Object> getPluginProperties(String pluginName) {
        final var keyPre = "plugin-%s-".formatted(pluginName);
        return this.properties.entrySet().stream()
                .filter(e -> e.getKey().startsWith(keyPre))
                .collect(Collectors.toMap(e -> e.getKey().substring(keyPre.length()), Map.Entry::getValue));
    }

    public enum Status {
        TYING,
        STOPPING,
        WAITING
    }
}
