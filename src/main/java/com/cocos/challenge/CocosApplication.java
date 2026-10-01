package com.cocos.challenge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport
public class CocosApplication {

    public static void main(String[] args) {
        var app = new SpringApplication(CocosApplication.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }
}
