package com.enumerate.intelligence;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.enumerate.intelligence.mapper")
public class IntelligenceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntelligenceApplication.class, args);
    }
}