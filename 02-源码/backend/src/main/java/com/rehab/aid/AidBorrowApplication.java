package com.rehab.aid;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 残障辅助器具借用与维护管理系统 启动类 */
@SpringBootApplication
@MapperScan("com.rehab.aid.mapper")
public class AidBorrowApplication {
    public static void main(String[] args) {
        SpringApplication.run(AidBorrowApplication.class, args);
    }
}
