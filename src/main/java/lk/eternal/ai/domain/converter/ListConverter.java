package lk.eternal.ai.domain.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lk.eternal.ai.util.Mapper;

import java.util.List;
import java.util.Map;

@Converter(autoApply = true)
public class ListConverter implements AttributeConverter<List<Object>, String> {


    @Override
    public String convertToDatabaseColumn(List<Object> value) {
        try {
            return value != null ? Mapper.getObjectMapper().writeValueAsString(value) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }

    @Override
    public List<Object> convertToEntityAttribute(String dbData) {
        try {
            return dbData != null ? Mapper.getObjectMapper().readValue(dbData, new TypeReference<>() {
            }) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error converting from JSON", e);
        }
    }
}