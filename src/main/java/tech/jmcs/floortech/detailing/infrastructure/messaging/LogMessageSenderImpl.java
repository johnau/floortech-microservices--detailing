package tech.jmcs.floortech.detailing.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.rabbitmq.*;
import tech.jmcs.floortech.common.amqp.LogServiceIndex;
import tech.jmcs.floortech.common.amqp.QueuesConfig;
import tech.jmcs.floortech.common.amqp.model.LogMessage;
import tech.jmcs.floortech.common.amqp.model.LogType;
import tech.jmcs.floortech.detailing.domain.service.RemoteLoggingService;

import java.util.Date;

@Service
@PropertySource("classpath:rabbitmq.properties")
public class LogMessageSenderImpl implements RemoteLoggingService {
    static final Logger log = LoggerFactory.getLogger(LogMessageSenderImpl.class);
    final QueuesConfig queuesConfig;
    final Sender sender;

    @Autowired
    public LogMessageSenderImpl(QueuesConfig queuesConfig, Sender sender) {
        this.queuesConfig = queuesConfig;
        this.sender = sender;
    }

    public void sendSystemError(String message) {
        var destinationInfo = queuesConfig.getTopics().get(LogServiceIndex.SYSTEM_ERROR_LOGS);
        log.info("Sending System Error Log Message: {} to: [Ex={}, Key={}]", message, destinationInfo.getExchange(), destinationInfo.getRoutingKey());
        var errorLog = LogMessage.builder()
                .type(LogType.SYSTEM_ERROR)
                .message(message)
                .timestamp(new Date())
                .source("Floortech.Quotes.Service")
                .username("")
                .build();
        sendLogMessage(destinationInfo.getExchange(), destinationInfo.getRoutingKey(), errorLog);
    }

    public void sendSystemInfo(String message) {
        var destinationInfo = queuesConfig.getTopics().get(LogServiceIndex.SYSTEM_INFO_LOGS);
        log.info("Sending System Info Log Message: {} to: [Ex={}, Key={}]", message, destinationInfo.getExchange(), destinationInfo.getRoutingKey());
        var infoLog = LogMessage.builder()
                .type(LogType.SYSTEM_INFO)
                .message(message)
                .timestamp(new Date())
                .source("Floortech.Quotes.Service")
                .username("")
                .build();
        sendLogMessage(destinationInfo.getExchange(), destinationInfo.getRoutingKey(), infoLog);
    }

    public void sendBusinessInfo(String message) {
        var destinationInfo = queuesConfig.getTopics().get(LogServiceIndex.BUSINESS_INFO_LOGS);
        log.info("Sending Business Info Log Message: {} to: [Ex={}, Key={}]", message, destinationInfo.getExchange(), destinationInfo.getRoutingKey());
        var infoLog = LogMessage.builder()
                .type(LogType.BUSINESS_INFO)
                .message(message)
                .timestamp(new Date())
                .source("Floortech.Quotes.Service")
                .username("")
                .build();
        sendLogMessage(destinationInfo.getExchange(), destinationInfo.getRoutingKey(), infoLog);
    }

    public void sendLogMessage(String exchange, String routingKey, LogMessage logMessage) {
        var mapper = new ObjectMapper();
        if (mapper.canSerialize(LogMessage.class)) {
            try {
                var jsonString = mapper.writeValueAsString(logMessage);
                var confirmations = sender.sendWithPublishConfirms(p -> new OutboundMessage(exchange, routingKey, jsonString.getBytes()));
                confirmations.subscribe(result -> {
                    if (result.isAck()) {
                        log.info("Message acknowledged by destination: " + result.getOutboundMessage().getBody().toString());
                    } else {
                        log.info("Message not acknowledged by destination: " + result.getOutboundMessage().getBody().toString());
                    }
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
