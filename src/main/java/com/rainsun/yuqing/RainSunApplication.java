package com.rainsun.yuqing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.rainsun.yuqing.mapper")
public class RainSunApplication {

    public static void main(String[] args) {
        SpringApplication.run(RainSunApplication.class, args);
    }

}
