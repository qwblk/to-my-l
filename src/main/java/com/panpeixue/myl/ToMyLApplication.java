package com.panpeixue.myl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.panpeixue.myl.mapper")
public class ToMyLApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToMyLApplication.class, args);
    }

}