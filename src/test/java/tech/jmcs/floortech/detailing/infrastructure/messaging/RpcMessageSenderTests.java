package tech.jmcs.floortech.detailing.infrastructure.messaging;

import com.rabbitmq.client.Delivery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.BlockingQueueConsumer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tech.jmcs.floortech.common.amqp.ExchangesConfig;
import tech.jmcs.floortech.common.amqp.QueuesConfig;
import tech.jmcs.floortech.common.amqp.rpc.RpcEndpoints;
import tech.jmcs.floortech.common.amqp.rpc.model.JobsInfoRpcPacket;
import tech.jmcs.floortech.common.dto.AddressDto;
import tech.jmcs.floortech.common.dto.FloortechJobDto;
import tech.jmcs.floortech.detailing.AppConfiguration;
import tech.jmcs.floortech.detailing.app.service.JobDataReplyData;
import tech.jmcs.floortech.detailing.infrastructure.IntegrationTestWithoutEurekaClient;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

@SpringBootTest
@Import({AppConfiguration.class, ExchangesConfig.class, QueuesConfig.class})
@ExtendWith(SpringExtension.class)
//@ExtendWith(OutputCaptureExtension.class)
@EnableAutoConfiguration
@ActiveProfiles("dev")
public class RpcMessageSenderTests implements RabbitMqTestContainer, IntegrationTestWithoutEurekaClient {
    @Autowired
    RpcMessageSender rpcMessageSender;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    ExchangesConfig exchangesConfig;
    @Autowired
    QueuesConfig queuesConfig;
    @Autowired
    AmqpAdmin amqpAdmin;
    @Autowired
    MessageConverter messageConverter;
    private static final RpcEndpoints GET_JOB_INFO = RpcEndpoints.GET_JOB_INFO;
    @BeforeEach
    public void setupExternalExchanges() {
        var jobsExchangeName = exchangesConfig.getNames().get("jobs");
        var exchange = ExchangeBuilder.topicExchange(jobsExchangeName).build();
        amqpAdmin.declareExchange(exchange);

        var queueName = GET_JOB_INFO.getRpcKey();
        var jobInfoRpcInfo = queuesConfig.getTopics().get(queueName);
        var routingKey = jobInfoRpcInfo.getRoutingKey();

        var queue = new Queue(queueName, true, false, false);
        amqpAdmin.declareQueue(queue);

        var binding = new Binding(queueName, Binding.DestinationType.QUEUE, jobsExchangeName, routingKey, null);
        amqpAdmin.declareBinding(binding);
    }

    @AfterEach
    public void clearExternalExchanges() {
        var jobsExchangeName = exchangesConfig.getNames().get("jobs");
        amqpAdmin.deleteExchange(jobsExchangeName);
        var jobsInfoRpcQueueName = GET_JOB_INFO.getRpcKey();
        amqpAdmin.deleteQueue(jobsInfoRpcQueueName);
    }

    @Test
    public void shouldReceiveAFloortechJobInfoResponse() {
        var jobUuid = UUID.randomUUID().toString();
        var queueName = GET_JOB_INFO.getRpcKey();
        var queue = queuesConfig.getTopics().get(queueName);

        Function<JobDataReplyData, String> replyFunction = (data) -> {
            var mapper = new ObjectMapper();
            var addressDto = new AddressDto(
                    "Unit 5",
                    "Test Street",
                    "Testburb",
                    "Testville",
                    "6969",
                    "WA",
                    "Australia");
            var jobDto = new FloortechJobDto();
            jobDto.setUuid(data.getUuid());
            jobDto.setJobNumber(20100);
            jobDto.setAddress(addressDto);

            String dtoJson = null;
            try {
                dtoJson = mapper.writeValueAsString(jobDto);
                Message reply = new Message(dtoJson.getBytes(), data.getMessageProperties());
                rabbitTemplate.send(data.getReplyTo(), reply);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Sent response message with FloortechJobDto");
            return "";
        };

        var messageListener = createMessageListenerContainer(queueName, replyFunction);
        var queues = getQueues(messageListener);

        Mono<Delivery> result = rpcMessageSender.sendJobInfoRpc(jobUuid, null);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        StepVerifier.create(result)
                .consumeNextWith(delivery -> {
                    System.out.println(count(queues));
                    var mapper = new ObjectMapper();
                    FloortechJobDto dto = null;
                    try {
                        dto = mapper.readValue(delivery.getBody(), FloortechJobDto.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Job information response received, Job number: " + dto.getJobNumber());
                    System.out.println("Job information response received, address: " + dto.getAddress().toString());
                    assertEquals(true, dto.getJobNumber() == 20100);
                    assertEquals(true, dto.getAddress().getCountry().equalsIgnoreCase("Australia"));
                })
                .verifyComplete();
        System.out.println("-------------------------------------------------------------------");
    }

    @Test
    public void shouldNotReceiveAFloortechJobInfoResponse() {
        var jobUuid = UUID.randomUUID().toString();
        var queueName = GET_JOB_INFO.getRpcKey();
        var queue = queuesConfig.getTopics().get(queueName);

        Function<JobDataReplyData, String> replyFunction = (data) -> {
            Message reply = new Message(new byte[0], data.getMessageProperties());
            rabbitTemplate.send(data.getReplyTo(), reply);
            return "";
        };

        var messageListener = createMessageListenerContainer(queueName, replyFunction);
//        var queues = getQueues(messageListener);

        Mono<Delivery> result = rpcMessageSender.sendJobInfoRpc(jobUuid, null);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        StepVerifier.create(result)
                .consumeNextWith(delivery -> {
                    System.out.println("Bytes length" + delivery.getBody().length);
                    System.out.println("BODY VALUE IS: " + new String(delivery.getBody()));
                    assertEquals(true, delivery.getBody().length == 0);
                    assertEquals(true, new String(delivery.getBody()).equals(""));
                })
                .verifyComplete();

        System.out.println("-------------------------------------------------------------------");
    }

    private static String count(List<BlockingQueue<?>> queues) {
        int n = 0;
        for (BlockingQueue<?> queue : queues) {
            n += queue.size();
        }
        return "Total queue size: " + n;
    }
    private SimpleMessageListenerContainer createMessageListenerContainer(String queueName, Function<JobDataReplyData, String> replyFunc) {
        var container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(rabbitTemplate.getConnectionFactory());
        container.setQueueNames(queueName);
        container.setPrefetchCount(10);
        container.setBatchSize(10);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setConcurrentConsumers(2);
//        var messageListenerAdapter = new MessageListenerAdapter(new SimpleAdapter(), new Jackson2JsonMessageConverter());
//        container.setMessageListener(messageListenerAdapter);
        container.setMessageListener(new TestMessageListener(rabbitTemplate, replyFunc));
        container.start();
        return container;
    }

    private class TestMessageListener implements MessageListener {
        RabbitTemplate rabbitTemplate;
        Function<JobDataReplyData, String> replyFunction;
        public TestMessageListener(RabbitTemplate rabbitTemplate, Function<JobDataReplyData, String> replyFunction) {
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
//            if (jobsInfoRpcPacket.getJobUuid().equalsIgnoreCase("XXX")) {
//                var addressDto = AddressDto.builder()
//                        .line1("Unit 5")
//                        .line2("Test Street")
//                        .suburb("Testburb")
//                        .city("Testville")
//                        .state("WA")
//                        .country("Australia")
//                        .postCode("6969")
//                        .build();
//                var jobDto = FloortechJobDto.builder()
//                        .uuid(jobsInfoRpcPacket.getJobUuid())
//                        .jobNumber(20100)
//                        .address(addressDto)
//                        .build();
//
////                rabbitTemplate.convertAndSend(exchange, replyTo, mapper.writeValueAsString(dto));
//                String dtoJson = null;
//                try {
//                    dtoJson = mapper.writeValueAsString(jobDto);
//                    Message reply = new Message(dtoJson.getBytes(), message.getMessageProperties());
//                    rabbitTemplate.send(replyTo, reply);
//                } catch (JsonProcessingException e) {
//                    throw new RuntimeException(e);
//                }
//                System.out.println("Sent response message with FloortechJobDto");
//            } else if (jobsInfoRpcPacket.getJobUuid().equalsIgnoreCase("YYY")) {
//                Message reply = new Message("".getBytes(), message.getMessageProperties());
//                rabbitTemplate.send(replyTo, reply);
//            }


        }
    }

    private class SimpleAdapter {
        public SimpleAdapter() {}
        @SuppressWarnings("unused")
        public void handleMessage(String input) {
            System.out.println("Received a message: " + input);
        }
        @SuppressWarnings("unused")
        public void handleMessage(LinkedHashMap<String, String> jsonMap) {
            System.out.println("Received a hashmap message" + jsonMap.toString());
            var mapper = new ObjectMapper();
            var clazz = mapper.convertValue(jsonMap, JobsInfoRpcPacket.class);
            System.out.println("Job UUID: " + clazz.getJobUuid());
        }
    }

    private static List<BlockingQueue<?>> getQueues(SimpleMessageListenerContainer container) {
        DirectFieldAccessor accessor = new DirectFieldAccessor(container);
        List<BlockingQueue<?>> queues = new ArrayList<BlockingQueue<?>>();
        @SuppressWarnings("unchecked")
        Set<BlockingQueueConsumer> consumers = (Set<BlockingQueueConsumer>) accessor.getPropertyValue("consumers");
        for (BlockingQueueConsumer consumer : consumers) {
            accessor = new DirectFieldAccessor(consumer);
            queues.add((BlockingQueue<?>) accessor.getPropertyValue("queue"));
        }
        return queues;
    }
}
