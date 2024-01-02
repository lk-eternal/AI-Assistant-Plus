package lk.eternal.ai.domain;

import com.fasterxml.jackson.core.type.TypeReference;
//import jakarta.persistence.*;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.util.Mapper;

import java.util.*;
import java.util.stream.Collectors;
//
//@Entity
//@Table(name = "users")
public class User {

//    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
    private final String id;

//    @Convert(converter = MessageListConverter.class)
    private final LinkedList<Message> messages;

    private final Map<String, Object> properties;

//    @Enumerated(EnumType.STRING)
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
                .orElseGet(Collections::emptyList);
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
//
//    @Converter
//    public class MessageListConverter implements AttributeConverter<List<Message>, String> {
//
//
//        @Override
//        public String convertToDatabaseColumn(List<Message> value) {
//            try {
//                return value != null ? Mapper.getObjectMapper().writeValueAsString(value) : null;
//            } catch (Exception e) {
//                throw new RuntimeException("Error converting to JSON", e);
//            }
//        }
//
//        @Override
//        public List<Message> convertToEntityAttribute(String dbData) {
//            try {
//                return dbData != null ? Mapper.getObjectMapper().readValue(dbData, new TypeReference<LinkedList<Message>>() {}) : null;
//            } catch (Exception e) {
//                throw new RuntimeException("Error converting from JSON", e);
//            }
//        }
//    }


}
