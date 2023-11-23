package lk.eternal.ai.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Mapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mapper.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        OBJECT_MAPPER.disable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        OBJECT_MAPPER.disable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        OBJECT_MAPPER.findAndRegisterModules();
    }

    public static ObjectMapper getObjectMapper() {
        return Mapper.OBJECT_MAPPER;
    }


    public static <T> T readValueNotError(String json, TypeReference<T> reference) {
        try {
            return OBJECT_MAPPER.readValue(json, reference);
        } catch (IOException e) {
            LOGGER.error("jackson mapper read value error", e);
            return null;
        }
    }

    public static <T> T readValueNotError(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            LOGGER.error("jackson mapper read value error", e);
            return null;
        }
    }

    public static String writeAsStringNotError(Object data) {
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (IOException e) {
            LOGGER.error("jackson mapper write as string error {}", data, e);
            return null;
        }
    }

}
