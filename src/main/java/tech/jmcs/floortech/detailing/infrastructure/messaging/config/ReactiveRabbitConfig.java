package tech.jmcs.floortech.detailing.infrastructure.messaging.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.*;
import tech.jmcs.floortech.common.amqp.ExchangesConfig;
import tech.jmcs.floortech.common.amqp.QueuesConfig;

import java.util.stream.Collectors;

@Configuration
@PropertySource("classpath:rabbitmq.properties")
public class ReactiveRabbitConfig {
    @Autowired
    AmqpAdmin amqpAdmin;
    @Autowired
    ExchangesConfig exchangesConfig;
    @Autowired
    QueuesConfig queuesConfig;

    @Value("${spring.rabbitmq.host}") String rabbitHost;
    @Value("${spring.rabbitmq.port}") String rabbitPort;
    @Value("${spring.rabbitmq.username}") String rabbitUsername;
    @Value("${spring.rabbitmq.password}") String rabbitPassword;

    @Bean()
    Mono<Connection> connectionMono(RabbitProperties rabbitProperties) {
        ConnectionFactory connectionFactory = new ConnectionFactory();

        var host = rabbitProperties.getHost();
        if (host == null || host.isEmpty()) {
            host = rabbitHost;
        }
        var port = rabbitProperties.getPort();
        if (port == null) {
            port = Integer.parseInt(rabbitPort);
        }
        var username = rabbitProperties.getUsername();
        if (username == null || username.isEmpty()) {
            username = rabbitUsername;
        }
        var password = rabbitProperties.getPassword();
        if (password == null || password.isEmpty()) {
            password = rabbitPassword;
        }

        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return Mono.fromCallable(() -> connectionFactory.newConnection("floortech-logs-service")).cache();
    }

    @Bean
    Sender sender(Mono<Connection> connectionMono) {
        return RabbitFlux.createSender(new SenderOptions().connectionMono(connectionMono));
    }

    @Bean
    Receiver receiver(Mono<Connection> connectionMono) {
        return RabbitFlux.createReceiver(new ReceiverOptions().connectionMono(connectionMono));
    }

    @PostConstruct
    public void init() {
    }

    /**
     * Only being used in tests currently? For RabbitTemplate
     * @return
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
