package tech.jmcs.floortech.detailing.infrastructure.persistence.dao;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

@Testcontainers
public interface MongoDBTestContainer {
    String DOCKER_IMAGE_NAME = "mongo:latest";

    @Container
    MongoDBContainer mongoDBContainer = new MongoDBContainer(DOCKER_IMAGE_NAME)
                                            .withExposedPorts(27017)
                                            .withStartupAttempts(5)
                                            .withStartupTimeout(Duration.ofMinutes(5));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.database", () -> "logs-test");
        registry.add("spring.data.mongodb.auto-index-creation", () -> "true");
        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.data.mongodb.port", () -> mongoDBContainer.getMappedPort(27017));
    }
}
