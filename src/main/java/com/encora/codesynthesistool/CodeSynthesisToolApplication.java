package com.encora.codesynthesistool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@SpringBootApplication
@EnableReactiveMongoRepositories
public class CodeSynthesisToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeSynthesisToolApplication.class, args);
    }
}
