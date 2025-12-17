package com.nei10u.tip;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类
 */
@SpringBootApplication
@MapperScan("com.nei10u.tip.mapper")
@EnableScheduling
public class TipBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TipBackendApplication.class, args);
    }
}
