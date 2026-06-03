package com.th3curiosity.studycards.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
@Testcontainers
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgreSQLContainer() {
        return new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("studycards_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withInitScript("db/init.sql");
    }

}
