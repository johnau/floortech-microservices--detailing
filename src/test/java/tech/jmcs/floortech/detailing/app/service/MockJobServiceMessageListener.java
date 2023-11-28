package tech.jmcs.floortech.detailing.app.service;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import tech.jmcs.floortech.common.amqp.rpc.model.JobsInfoRpcPacket;

import java.io.IOException;
import java.util.function.Function;

class MockJobServiceMessageListener implements MessageListener {
    RabbitTemplate rabbitTemplate;
    Function<JobDataReplyData, String> replyFunction;

    public MockJobServiceMessageListener(RabbitTemplate rabbitTemplate, Function<JobDataReplyData, String> replyFunction) {
        this.rabbitTemplate = rabbitTemplate;
        this.replyFunction = replyFunction;
    }

    @Override
    public void onMessage(Message message) {
        System.out.println("Received msg: " + new String(message.getBody()));
        var replyTo = message.getMessageProperties().getReplyTo();
        var correlationId = message.getMessageProperties().getCorrelationId();
        var queue = message.getMessageProperties().getConsumerQueue();
        var exchange = message.getMessageProperties().getReceivedExchange();
        var headers = message.getMessageProperties().getHeaders();
        var replyToAddress = message.getMessageProperties().getReplyToAddress();
        var receivedRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
        var deliveryTag = message.getMessageProperties().getDeliveryTag();

        System.out.println("\t -Reply to:" + replyTo);
        System.out.println("\t -Correlation id:" + correlationId);
        System.out.println("\t -Consumer Queue:" + queue);
        System.out.println("\t -Received exchange:" + exchange);
        System.out.println("\t -headers:" + headers);
        System.out.printf("\t -replyToAddress: %s | on Exchange: %s\n", replyToAddress.getRoutingKey(), replyToAddress.getExchangeName());
        System.out.println("\t -receivedRoutingKey:" + receivedRoutingKey);
        System.out.println("\t -deliveryTag:" + deliveryTag);

        var mapper = new ObjectMapper();
        JobsInfoRpcPacket jobsInfoRpcPacket = null;
        try {
            jobsInfoRpcPacket = mapper.readValue(message.getBody(), JobsInfoRpcPacket.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JobDataReplyData rdc = new JobDataReplyData(
                jobsInfoRpcPacket.getJobUuid(),
                20100,
                replyTo,
                message.getMessageProperties()
        );
        replyFunction.apply(rdc);
    }
}
