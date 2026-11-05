package com.github.b1gsal.carsharingservice.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class CustomMySqlContainer {

    @ServiceConnection
    protected static final MySQLContainer<?> MY_SQL_CONTAINER;

    static {
        System.setProperty("api.version", "1.40");
        MY_SQL_CONTAINER = new MySQLContainer<>("mysql:8.0.33");
        MY_SQL_CONTAINER.start();
    }
}
