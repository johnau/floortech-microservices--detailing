package tech.jmcs.floortech.detailing.infrastructure;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public interface IntegrationTestWithoutEurekaClient {
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> false);
    }
}
