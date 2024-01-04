package lk.eternal.ai.domain.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lk.eternal.ai.dto.req.Message;
import lk.eternal.ai.util.Mapper;

import java.util.LinkedList;
import java.util.List;

@Converter
public class MessageListConverter implements AttributeConverter<List<Message>, String> {


    @Override
    public String convertToDatabaseColumn(List<Message> value) {
        try {
            return value != null ? Mapper.getObjectMapper().writeValueAsString(value) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }

    @Override
    public List<Message> convertToEntityAttribute(String dbData) {
        try {
            return dbData != null ? Mapper.getObjectMapper().readValue(dbData, new TypeReference<LinkedList<Message>>() {
            }) : null;
        } catch (Exception e) {
            throw new RuntimeException("Error converting from JSON", e);
        }
    }
}