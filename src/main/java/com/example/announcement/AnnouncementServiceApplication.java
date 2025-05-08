package com.example.announcement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableDiscoveryClient  // 注册到 Nacos 的关键注解
public class AnnouncementServiceApplication {
    public static void main(String[] args) {

        SpringApplication.run(AnnouncementServiceApplication.class, args);
    }
}