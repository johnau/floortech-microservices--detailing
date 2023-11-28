package tech.jmcs.floortech.detailing.infrastructure.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tech.jmcs.floortech.common.amqp.ExchangesConfig;
import tech.jmcs.floortech.common.amqp.QueuesConfig;
import tech.jmcs.floortech.detailing.AppConfiguration;

@SpringBootTest
@Import({AppConfiguration.class, ExchangesConfig.class, QueuesConfig.class})
@ExtendWith(SpringExtension.class)
@ExtendWith(OutputCaptureExtension.class)
@EnableAutoConfiguration
@ActiveProfiles("dev")
public class DetailingMessagingTests {
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    ReactiveMongoTemplate mongoTemplate;
    @Autowired
    ExchangesConfig exchangesConfig;
    @Autowired
    QueuesConfig queuesConfig;

    @BeforeEach
    public void clearDb() {

    }
}
