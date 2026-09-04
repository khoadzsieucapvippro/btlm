package com.elearning;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.util.Map;

/**
 * Global Testcontainers ApplicationContextInitializer.
 * <p>
 * Starts a singleton MySQL 8.4 container once for all Spring Boot integration tests
 * and dynamically injects connection parameters into the Spring Environment.
 */
public class TestcontainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        // Ensure Docker API version compatibility with modern Docker Engine (26+, 28+, 29+) on Windows
        if (System.getProperty("api.version") == null) {
            System.setProperty("api.version", "1.44");
        }
        if (System.getProperty("DOCKER_API_VERSION") == null) {
            System.setProperty("DOCKER_API_VERSION", "1.44");
        }

        MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.4.0")
                .withDatabaseName("elearning_test")
                .withUsername("test")
                .withPassword("test")
                .withCommand("--lower-case-table-names=1")
                .withReuse(true);

        MYSQL_CONTAINER.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Map<String, Object> properties = Map.of(
                "spring.datasource.url", MYSQL_CONTAINER.getJdbcUrl(),
                "spring.datasource.username", MYSQL_CONTAINER.getUsername(),
                "spring.datasource.password", MYSQL_CONTAINER.getPassword(),
                "spring.datasource.driver-class-name", MYSQL_CONTAINER.getDriverClassName()
        );
        applicationContext.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("testcontainers-properties", properties));
    }

    public static MySQLContainer<?> getContainer() {
        return MYSQL_CONTAINER;
    }
}
