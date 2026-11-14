package com.mouse.mes.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class FileUploadConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(2));
        // Leave room for multipart headers/title fields around a legitimate 2MB file.
        factory.setMaxRequestSize(DataSize.ofMegabytes(3));
        return factory.createMultipartConfig();
    }
}
