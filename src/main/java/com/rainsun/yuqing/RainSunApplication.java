package com.rainsun.yuqing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.rainsun.yuqing.mapper")
@EnableScheduling
public class RainSunApplication {

    public static void main(String[] args) {
        SpringApplication.run(RainSunApplication.class, args);
    }

}
