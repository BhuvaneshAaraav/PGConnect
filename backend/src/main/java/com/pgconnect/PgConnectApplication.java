package com.pgconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class PgConnectApplication {
    public static void main(String[] args) {
        SpringApplication.run(PgConnectApplication.class, args);
    }
}
