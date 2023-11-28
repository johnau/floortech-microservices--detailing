package tech.jmcs.floortech.detailing.infrastructure.messaging;

import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;

@Configuration
public class RabbitMqTestContainerConfig {
    static String DOCKER_IMAGE_NAME = "rabbitmq:3.11";

    @Container
    static RabbitMQContainer rabbitMqContainer = new RabbitMQContainer(DOCKER_IMAGE_NAME);
//            .withExchange("ft.logs", "topic")
//            .withQueue("log");

    static {
        rabbitMqContainer.start();
        var host = rabbitMqContainer.getHost();
        var mappedPort = rabbitMqContainer.getAmqpPort();
        var username = rabbitMqContainer.getAdminUsername();
        var password = rabbitMqContainer.getAdminPassword();

        System.setProperty("spring.rabbitmq.host", host);
        System.setProperty("spring.rabbitmq.port", String.valueOf(mappedPort));
        System.setProperty("spring.rabbitmq.username", username);
        System.setProperty("spring.rabbitmq.password", password);
    }

//    @DynamicPropertySource
//    static void registerProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.rabbitmq.host", rabbitMqContainer::getHost);
//        registry.add("spring.rabbitmq.port", rabbitMqContainer::getAmqpPort);
//        registry.add("spring.rabbitmq.username", rabbitMqContainer::getAdminUsername);
//        registry.add("spring.rabbitmq.password", rabbitMqContainer::getAdminPassword);
//    }
}