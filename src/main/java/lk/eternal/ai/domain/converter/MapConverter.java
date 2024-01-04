package lk.eternal.ai.domain.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lk.eternal.ai.util.Mapper;

import java.util.Map;

@Converter(autoApply = true)
public class MapConverter implements AttributeConverter<Map<String, Object>, String> {


    @Override
    public String convertToDatabaseColumn(Map<String, Object> value) {
        try {
            return value != null ? Mapper.getObjectMapper().writeValueAsString(value) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        try {
            return dbData != null ? Mapper.getObjectMapper().readValue(dbData, new TypeReference<>() {
            }) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error converting from JSON", e);
        }
    }
}