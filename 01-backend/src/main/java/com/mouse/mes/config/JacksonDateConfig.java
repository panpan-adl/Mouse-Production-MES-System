package com.mouse.mes.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
public class JacksonDateConfig {

    private static final String[] DATE_FORMATS = {
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS"
    };

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Date.class, new MultiFormatDateDeserializer());
        objectMapper.registerModule(module);
        
        return objectMapper;
    }

    public static class MultiFormatDateDeserializer extends JsonDeserializer<Date> {

        @Override
        public Date deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String dateStr = parser.getText().trim();
            if (dateStr.isEmpty()) {
                return null;
            }
            
            for (String format : DATE_FORMATS) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format);
                    sdf.setLenient(false);
                    return sdf.parse(dateStr);
                } catch (ParseException ignored) {
                }
            }
            
            throw new IOException("无法解析日期: " + dateStr + ", 支持的格式: " + String.join(", ", DATE_FORMATS));
        }
    }
}
