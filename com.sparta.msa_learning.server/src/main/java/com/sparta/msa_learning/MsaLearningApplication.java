package com.sparta.msa_learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class MsaLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsaLearningApplication.class, args);
    }

}
