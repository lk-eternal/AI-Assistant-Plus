package lk.eternal.ai.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lk.eternal.ai.domain.converter.ListConverter;
import lk.eternal.ai.domain.converter.MapConverter;
import lk.eternal.ai.domain.converter.MessageListConverter;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.util.PasswordUtil;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "lk_users")
public class User extends BaseEntity{

    @Column(unique = true)
    private String email;

    private String password;

    private boolean gpt4Enable;

    @Convert(converter = MessageListConverter.class)
    private LinkedList<Message> messages;

    @Convert(converter = MapConverter.class)
    private Map<String, Object> properties;

    @Transient
    private Status status;

    public User() {
        super(UUID.randomUUID());
        this.messages = new LinkedList<>();
        this.properties = new HashMap<>();
        this.status = Status.WAITING;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = PasswordUtil.hashPassword(password);
    }

    public String getAiModel() {
        return getProperty("aiModel").map(Object::toString).orElse(null);
    }

    public String getPluginModel() {
        return getProperty("pluginModel").map(Object::toString).orElse(null);
    }

    @SuppressWarnings("unchecked")
    public List<String> getPlugins() {
        return (List<String>) getProperty("plugins").filter(ps -> ps instanceof List).orElse(null);
    }

    @JsonIgnore
    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    public void setMessages(LinkedList<Message> messages) {
        this.messages = messages;
    }

    public LinkedList<Message> getMessages() {
        return messages;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public void putProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    @JsonIgnore
    public Optional<Object> getProperty(String key) {
        return Optional.ofNullable(this.properties.get(key));
    }

    public boolean isGpt4Enable() {
        return gpt4Enable;
    }

    public void setGpt4Enable(boolean gpt4Enable) {
        this.gpt4Enable = gpt4Enable;
    }

    public void clear() {
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

    public boolean isDbUser() {
        return StringUtils.hasText(email);
    }

}
