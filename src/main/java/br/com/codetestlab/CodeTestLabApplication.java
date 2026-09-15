package br.com.codetestlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CodeTestLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeTestLabApplication.class, args);
    }
}
