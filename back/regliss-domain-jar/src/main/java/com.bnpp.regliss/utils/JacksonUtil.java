package com.bnpp.regliss.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JacksonUtil {

    public static Optional<String> getSerializedValue(Object parameters) {
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            return Optional.of(objectMapper.writeValueAsString(parameters));
        } catch(JsonProcessingException e) {
            log.error("Could not serialize batch executions parameters: {} ", e.getMessage());
            return Optional.empty();
        }
    }

    public static <T> Optional<T> getDeserializedValue(String json, Class<T> valueType) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            return Optional.of(objectMapper.readValue(json, valueType));
        } catch (IOException e) {
            log.error("Cannot deserialize JSON: {} to class: {}.", json, valueType, e);
            return Optional.empty();
        }
    }

    public static <T> Optional<List<T>> getTypedDeserializedValue(String json, Class collectionType, Class<T> valueType) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            CollectionType javaType = objectMapper.getTypeFactory().constructCollectionType(collectionType, valueType);
            return Optional.of(objectMapper.readValue(json, javaType));
        } catch (IOException e) {
            log.error("Cannot deserialize JSON: {} to class: {}.", json, valueType, e);
            return Optional.empty();
        }
    }
}
