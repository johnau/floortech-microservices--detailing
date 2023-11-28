package tech.jmcs.floortech.detailing.infrastructure.messaging;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public interface RabbitMqTestContainer {
    String DOCKER_IMAGE_NAME = "rabbitmq:3.11";

    @Container
    RabbitMQContainer rabbitMqContainer = new RabbitMQContainer(DOCKER_IMAGE_NAME);
//            .withExchange("ft.logs", "topic")
//            .withQueue("log");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMqContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMqContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMqContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMqContainer::getAdminPassword);
    }


}
