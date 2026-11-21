package com.bnpp.regliss.web.boot;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;

/serial/
public class CustomJacksonObjectMapper extends ObjectMapper {

    public CustomJacksonObjectMapper() {
        super();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new CustomLocalDateTimeSerializer());
        javaTimeModule.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
        javaTimeModule.addDeserializer(LocalDate.class, new CustomLocalDateDeserializer());
        this.registerModule(javaTimeModule);
        // TODO eventually move to Spring Boot central configuration
        this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.configure(SerializationFeature.INDENT_OUTPUT, true);
        this.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    }

    class CustomLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value.toInstant(ZoneOffset.UTC)));

        }
    }

    class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            if(p.getValueAsString().isEmpty()){
                return null;
            }
            return LocalDateTime.ofInstant(Instant.parse(p.getValueAsString()), ZoneOffset.UTC);
        }

    }
    class CustomLocalDateDeserializer extends LocalDateDeserializer {

        public CustomLocalDateDeserializer() {
            super(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        @Override
        public LocalDate deserialize(JsonParser parser, DeserializationContext ctxt)
                throws IOException {

            if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return Instant.ofEpochMilli(parser.readValueAs(Long.class)).atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                return super.deserialize(parser, ctxt);
            }
        }

    }
}

