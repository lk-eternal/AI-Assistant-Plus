package lk.eternal.ai.dto.req;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.Collections;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record Parameters(String type, Properties properties, List<String> required) {

    public Parameters(Properties properties, List<String> required) {
        this("object", properties, required);
    }

    public static Parameters singleton(String name, String type, String description) {
        final var properties = new Properties();
        properties.put(name, new Parameter(type, description));
        return new Parameters(properties, Collections.singletonList(name));
    }
}

