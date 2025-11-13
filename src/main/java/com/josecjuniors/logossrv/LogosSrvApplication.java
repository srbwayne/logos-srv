package com.josecjuniors.logossrv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LogosSrvApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogosSrvApplication.class, args);
    }

}
