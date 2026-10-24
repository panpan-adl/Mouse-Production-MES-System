package com.mouse.mes;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.mouse.mes.mapper")
@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class MouseMesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MouseMesApplication.class, args);
    }
}
