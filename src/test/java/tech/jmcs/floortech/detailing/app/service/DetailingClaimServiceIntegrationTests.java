package tech.jmcs.floortech.detailing.app.service;

import com.rabbitmq.client.Delivery;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tech.jmcs.floortech.common.amqp.ExchangesConfig;
import tech.jmcs.floortech.common.amqp.LogServiceIndex;
import tech.jmcs.floortech.common.amqp.QueuesConfig;
import tech.jmcs.floortech.common.amqp.rpc.RpcEndpoints;
import tech.jmcs.floortech.common.dto.AddressDto;
import tech.jmcs.floortech.common.dto.BuilderClientDto;
import tech.jmcs.floortech.common.dto.EngineerDto;
import tech.jmcs.floortech.common.dto.FloortechJobDto;
import tech.jmcs.floortech.detailing.AppConfiguration;
import tech.jmcs.floortech.detailing.app.service.exception.DetailingClaimServiceException;
import tech.jmcs.floortech.detailing.domain.configs.DetailingStatus;
import tech.jmcs.floortech.detailing.domain.service.FileStorageService;
import tech.jmcs.floortech.detailing.infrastructure.IntegrationTestWithoutEurekaClient;
import tech.jmcs.floortech.detailing.infrastructure.messaging.RabbitMqTestContainer;
import tech.jmcs.floortech.detailing.infrastructure.messaging.RpcMessageSender;
import tech.jmcs.floortech.detailing.infrastructure.messaging.exception.RemoteServiceException;
import tech.jmcs.floortech.detailing.infrastructure.persistence.dao.DetailingClaimDao;
import tech.jmcs.floortech.detailing.infrastructure.persistence.dao.MongoDBTestContainer;
import tech.jmcs.floortech.detailing.infrastructure.persistence.entity.DetailingClaimEntity;
import tech.jmcs.floortech.detailing.infrastructure.persistence.entity.FileSetEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static tech.jmcs.floortech.detailing.domain.configs.DetailingStatus.COMPLETED;
import static tech.jmcs.floortech.detailing.domain.configs.DetailingStatus.PAUSED;

@SpringBootTest
@DirtiesContext
@Import({AppConfiguration.class, ExchangesConfig.class, QueuesConfig.class})
@ExtendWith(SpringExtension.class)
@ExtendWith(OutputCaptureExtension.class)
@EnableAutoConfiguration
@ActiveProfiles("dev")
public class DetailingClaimServiceIntegrationTests implements RabbitMqTestContainer, MongoDBTestContainer, IntegrationTestWithoutEurekaClient {
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
    FileStorageService fileStorage;
    @Autowired
    private DetailingClaimService detailingClaimService;
    @Autowired
    private DetailingClaimDao detailingClaimDao;
    @Autowired
    private DetailingFileService detailingFileService;
    private static final RpcEndpoints GET_JOB_INFO = RpcEndpoints.GET_JOB_INFO;

    @BeforeEach
    public void prepare() {
        detailingClaimDao.deleteAll().block();
        setupJobsExchange();
//        setupLogsExchange();
    }

    /**
     * NOTE: THERE IS NO TEST FOR THE LOG MESSAGES THAT ARE SENT
     * -- Topography of the message system needs to be changed so that all messages are sent to the same exchange
     * -- Or need to work out how to run two exchange containers simultaneously in tests
     */

    private void setupJobsExchange() {
        var jobsExchangeName = exchangesConfig.getNames().get("jobs");
        var jobsExchange = ExchangeBuilder.topicExchange(jobsExchangeName).build();
        amqpAdmin.declareExchange(jobsExchange);

        var name = GET_JOB_INFO.getRpcKey();
        var jobInfoRpcInfo = queuesConfig.getTopics().get(name);
        var routingKey = jobInfoRpcInfo.getRoutingKey();

        var queue = new Queue(name, true, false, false);
        amqpAdmin.declareQueue(queue);

        var binding = new Binding(name, Binding.DestinationType.QUEUE, jobsExchangeName, routingKey, null);
        amqpAdmin.declareBinding(binding);
    }

    private void setupLogsExchange() {
        var logsExchangeName = exchangesConfig.getNames().get("logs");
        var logsExchange = ExchangeBuilder.topicExchange(logsExchangeName).build();
        amqpAdmin.declareExchange(logsExchange);

        var name = LogServiceIndex.BUSINESS_INFO_LOGS;
        var destinationInfo = queuesConfig.getTopics().get(name);
        var routingKey = destinationInfo.getRoutingKey();

        var queue = new Queue(name, true, false, false);
        amqpAdmin.declareQueue(queue);

        var binding = new Binding(name, Binding.DestinationType.QUEUE, logsExchangeName, routingKey, null);
        amqpAdmin.declareBinding(binding);
    }

    @AfterEach
    public void cleanup() {
        var jobsExchangeName = exchangesConfig.getNames().get("jobs");
        amqpAdmin.deleteExchange(jobsExchangeName);
        var jobsInfoRpcQueueName = GET_JOB_INFO.getRpcKey();
        amqpAdmin.deleteQueue(jobsInfoRpcQueueName);

//        var logsExchangeName = exchangesConfig.getNames().get("logs");
//        amqpAdmin.deleteExchange(logsExchangeName);
//        var businessInfoLogsQueueName = LogServiceIndex.BUSINESS_INFO_LOGS;
//        amqpAdmin.deleteQueue(businessInfoLogsQueueName);
    }

    // <editor-fold desc="Infrastructure tests">
    @Test
    public void mustReceiveAFloortechJobInfoResponse() {
        var jobUuid = UUID.randomUUID().toString();
        var queueName = GET_JOB_INFO.getRpcKey();
        var queue = queuesConfig.getTopics().get(queueName);
        Function<JobDataReplyData, String> replyFunction = (data) -> {
            var mapper = new ObjectMapper();
            var addressDto = new AddressDto("Unit 5", "Test Street", "Testburb", "Testville",
                                        "6969", "WA", "AU");
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

        var messageListener = createMockJobServiceMessageListenerContainer(queueName, replyFunction);
        var queues = TestHelpers.getQueues(messageListener);

        Mono<Delivery> result = rpcMessageSender.sendJobInfoRpc(jobUuid, null);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        StepVerifier.create(result)
                .consumeNextWith(delivery -> {
                    System.out.println(TestHelpers.count(queues));
                    var mapper = new ObjectMapper();
                    FloortechJobDto dto;
                    try {
                        dto = mapper.readValue(delivery.getBody(), FloortechJobDto.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Job information response received, Job number: " + dto.getJobNumber());
                    System.out.println("Job information response received, address: " + dto.getAddress().toString());
                    assertEquals(true, dto.getJobNumber() == 20100);
                    assertEquals(true, dto.getAddress().getCountry().equalsIgnoreCase("AU"));
                })
                .verifyComplete();
        System.out.println("-------------------------------------------------------------------");
        messageListener.stop();
        messageListener.shutdown();
    }
    // </editor-fold>

    // <editor-fold desc="Testing Create Claim">
    @Test
    @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
    public void mustCreateClaim() {
        var jobId = "EXAMPLE-ID-JOB-001A";

        Function<JobDataReplyData, String> replyFunction = (data) -> {
            System.out.println("Start of sleep: " + new Date());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("End of sleep: " + new Date());
            var mapper = new ObjectMapper();
            var addressDto = new AddressDto(
                    "Unit 5",
                    "Test Street",
                    "Testburb",
                    "Testville",
                    "6969",
                    "WA",
                    "AU");
            var clientDto = new BuilderClientDto();
            clientDto.setUuid("CLIENT-000-000-001");
            clientDto.setCompanyName("Building WA");
            var engineerDto = new EngineerDto();
            engineerDto.setUuid("ENGINEER-000-000-001");
            engineerDto.setCompanyName("Engineering WA");
            var jobDto = new FloortechJobDto();
            jobDto.setUuid(data.getUuid());
            jobDto.setJobNumber(20100);
            jobDto.setAddress(addressDto);
            jobDto.setClient(clientDto);
            jobDto.setEngineer(engineerDto);

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

        var jobInfoQueueName = GET_JOB_INFO.getRpcKey();
        var jobsMessageListener = createMockJobServiceMessageListenerContainer(jobInfoQueueName, replyFunction);
        var queues = TestHelpers.getQueues(jobsMessageListener);

        var resultMono = detailingClaimService.claimDetailingJob("test_user", jobId);

        StepVerifier.create(resultMono)
                .consumeNextWith(getDetailingClaimDto -> {
                    var status = getDetailingClaimDto.status();
                    var jobNumber = getDetailingClaimDto.floortechJobNumber();
                    var clientId = getDetailingClaimDto.jobClientId();
                    var clientName = getDetailingClaimDto.jobClientName();
                    var claimedByUsername = getDetailingClaimDto.claimedByStaffUsername();
                    assertEquals(DetailingStatus.STARTED, status);
                    assertEquals(Integer.valueOf(20100), jobNumber);
                    assertEquals("CLIENT-000-000-001", clientId);
                    assertEquals("Building WA", clientName);
                    assertEquals("test_user", claimedByUsername);
                })
                .verifyComplete();
        jobsMessageListener.stop();
        jobsMessageListener.shutdown();
    }

    @Test
    @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
    public void mustCreateClaimBecauseExistingClaimIsCancelled() {
        var jobId = "EXAMPLE-ID-JOB-001A";
        var existingEntity = new DetailingClaimEntity();
        existingEntity.setJobId(jobId);
        existingEntity.setClaimedByStaffUsername("different_user");
        existingEntity.setStatus(DetailingStatus.CANCELLED);
                
        detailingClaimDao.save(existingEntity).block();

        var queueName = GET_JOB_INFO.getRpcKey();
        Function<JobDataReplyData, String> replyFunction = (data) -> {
            System.out.println("Start of sleep: " + new Date());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("End of sleep: " + new Date());
            var mapper = new ObjectMapper();
            var addressDto = new AddressDto(
                    "Unit 5",
                    "Test Street",
                    "Testburb",
                    "Testville",
                    "6969",
                    "WA",
                    "AU");
            var clientDto = new BuilderClientDto();
            clientDto.setUuid("CLIENT-000-000-001");
            clientDto.setCompanyName("Building WA");
            var engineerDto = new EngineerDto();
            engineerDto.setUuid("ENGINEER-000-000-001");
            engineerDto.setCompanyName("Engineering WA");
            var jobDto = new FloortechJobDto();
            jobDto.setUuid(data.getUuid());
            jobDto.setJobNumber(20100);
            jobDto.setAddress(addressDto);
            jobDto.setClient(clientDto);
            jobDto.setEngineer(engineerDto);

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

        var messageListener = createMockJobServiceMessageListenerContainer(queueName, replyFunction);
        var queues = TestHelpers.getQueues(messageListener);

        var resultMono = detailingClaimService.claimDetailingJob("test_user", jobId);

        StepVerifier.create(resultMono)
                .consumeNextWith(getDetailingClaimDto -> {
                    var status = getDetailingClaimDto.status();
                    var jobNumber = getDetailingClaimDto.floortechJobNumber();
                    var clientId = getDetailingClaimDto.jobClientId();
                    var clientName = getDetailingClaimDto.jobClientName();
                    var claimedByUsername = getDetailingClaimDto.claimedByStaffUsername();
                    assertEquals(DetailingStatus.STARTED, status);
                    assertEquals(Integer.valueOf(20100), jobNumber);
                    assertEquals("CLIENT-000-000-001", clientId);
                    assertEquals("Building WA", clientName);
                    assertEquals("test_user", claimedByUsername);
                })
                .verifyComplete();
        messageListener.stop();
        messageListener.shutdown();
    }

    @Test
    @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
    public void mustErrorBecauseAlreadyClaimed() {
        var jobId = "EXAMPLE-ID-JOB-001A";
        var existingEntity = new DetailingClaimEntity();
        existingEntity.setJobId(jobId);
        existingEntity.setClaimedByStaffUsername("test_user");
        existingEntity.setFloortechJobNumber(123456);
        existingEntity.setJobClientId("X");
        existingEntity.setJobClientName("Y");
        existingEntity.setStatus(DetailingStatus.PAUSED);
        detailingClaimDao.save(existingEntity).block();

        var resultMono = detailingClaimService.claimDetailingJob("test_user", jobId);

        StepVerifier.create(resultMono)
                .expectError()
                .verify();

    }

    @Test
    @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
    public void mustNotClaimAlreadyClaimed() {
        var jobId = "EXAMPLE-ID-JOB-001A";
        var existingEntity = new DetailingClaimEntity();
        existingEntity.setJobId(jobId);
        existingEntity.setClaimedByStaffUsername("different_user");
        existingEntity.setStatus(DetailingStatus.STARTED);
        detailingClaimDao.save(existingEntity).block();

        var queueName = GET_JOB_INFO.getRpcKey();

        var resultMono = detailingClaimService.claimDetailingJob("test_user", jobId);

        StepVerifier.create(resultMono)
                .consumeErrorWith(error -> {
                    var msg = error.getMessage().toLowerCase();
                    System.out.println(error.getMessage());
                    assertEquals(true, msg.contains("already claimed"));
                    assertEquals(true, error instanceof DetailingClaimServiceException);
                })
                .verify();
    }

    @Test
    @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
    public void mustProcessOwnExistingUnverifiedClaim() {
        var jobId = "EXAMPLE-ID-JOB-001A";
        var existingEntity = new DetailingClaimEntity();
        existingEntity.setJobId(jobId);
        existingEntity.setClaimedByStaffUsername("test_user");
        existingEntity.setStatus(DetailingStatus.UNVERIFIED);
        var savedExistingExample = detailingClaimDao.save(existingEntity).block();
        System.out.println("Saved entity id: " + savedExistingExample.getId());
        System.out.println("Saved entity created: " + savedExistingExample.getCreatedDate());
        var existingLookupByCompoundId = detailingClaimDao.findByCompoundId(savedExistingExample.getJobId(), savedExistingExample.getClaimedByStaffUsername(), savedExistingExample.getCreatedDate()).block();
        assertNotEquals(null, existingLookupByCompoundId);
        System.out.println("ID: " + existingLookupByCompoundId.getId());

        var queueName = GET_JOB_INFO.getRpcKey();
        Function<JobDataReplyData, String> replyFunction = (data) -> {
            System.out.println("Start of sleep: " + new Date());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("End of sleep: " + new Date());
            var mapper = new ObjectMapper();
            var addressDto = new AddressDto(
                    "Unit 5",
                    "Test Street",
                    "Testburb",
                    "Testville",
                    "6969",
                    "WA",
                    "AU");
            var clientDto = new BuilderClientDto();
            clientDto.setUuid("CLIENT-000-000-001");
            clientDto.setCompanyName("Building WA");
            var engineerDto = new EngineerDto();
            engineerDto.setUuid("ENGINEER-000-000-001");
            engineerDto.setCompanyName("Engineering WA");
            var jobDto = new FloortechJobDto();
            jobDto.setUuid(data.getUuid());
            jobDto.setJobNumber(20100);
            jobDto.setAddress(addressDto);
            jobDto.setClient(clientDto);
            jobDto.setEngineer(engineerDto);

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

        var messageListener = createMockJobServiceMessageListenerContainer(queueName, replyFunction);
        var queues = TestHelpers.getQueues(messageListener);

       var resultMono = detailingClaimService.claimDetailingJob("test_user", jobId);

        StepVerifier.create(resultMono)
                .consumeNextWith(dto -> {
                    var status = dto.status();
                    var jobNumber = dto.floortechJobNumber();
                    var clientId = dto.jobClientId();
                    var clientName = dto.jobClientName();
                    var claimedByUsername = dto.claimedByStaffUsername();
                    assertEquals(DetailingStatus.STARTED, status);
                    assertEquals(Integer.valueOf(20100), jobNumber);
                    assertEquals("CLIENT-000-000-001", clientId);
                    assertEquals("Building WA", clientName);
                    assertEquals("test_user", claimedByUsername);
                })
                .verifyComplete();

        messageListener.stop();
        messageListener.shutdown();
    }

    @Test
    @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
    public void mustNotGetResponseBecauseChannelClosedAndReturnError() {
        var jobId = "TESTJOB-00001B";

        var resultMono = detailingClaimService.claimDetailingJob("test_user", jobId);

        StepVerifier.create(resultMono)
                .consumeErrorWith(error -> {
                    var msg = error.getMessage().toLowerCase();
                    System.out.println("Error: " + error.getMessage());
                    assertEquals(true, msg.contains("no response"));
                    assertEquals(true, error instanceof RemoteServiceException);
                })
                .verify();
    }

    @Test
    @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
    public void mustNotGetResponseBecauseTimeoutAndReturnError() {
        var jobId = "TESTJOB-00001B";
        var queueName = GET_JOB_INFO.getRpcKey();
        Function<JobDataReplyData, String> replyFunction = (data) -> {
            System.out.println("Start of sleep: " + new Date());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("End of sleep: " + new Date());
            var mapper = new ObjectMapper();
            var addressDto = new AddressDto(
                    "Unit 5",
                    "Test Street",
                    "Testburb",
                    "Testville",
                    "6969",
                    "WA",
                    "AU");
            var clientDto = new BuilderClientDto();
            clientDto.setUuid("CLIENT-000-000-001");
            clientDto.setCompanyName("Building WA");
            var engineerDto = new EngineerDto();
            engineerDto.setUuid("ENGINEER-000-000-001");
            engineerDto.setCompanyName("Engineering WA");
            var jobDto = new FloortechJobDto();
            jobDto.setUuid(data.getUuid());
            jobDto.setJobNumber(20100);
            jobDto.setAddress(addressDto);
            jobDto.setClient(clientDto);
            jobDto.setEngineer(engineerDto);

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

        var messageListener = createMockJobServiceMessageListenerContainer(queueName, replyFunction);
        var queues = TestHelpers.getQueues(messageListener);

        var resultMono = detailingClaimService.claimDetailingJob("test_user", jobId);

        StepVerifier.create(resultMono)
                .consumeErrorWith(error -> {
                    var msg = error.getMessage().toLowerCase();
                    System.out.println("Error: " + error.getMessage());
                    assertEquals(true, msg.contains("no response"));
                    assertEquals(true, msg.contains("timeout"));
                    assertEquals(true, error instanceof RemoteServiceException);
                })
                .verify();

        messageListener.stop();
        messageListener.shutdown();
    }
    // </editor-fold>

    @Nested
    class NestedDbTests {
        private Map<String, DetailingClaimEntity> ids = new HashMap();

        @BeforeEach
        public void setupDb() {
            detailingClaimDao.deleteAll().block();
            setupClaims();
            System.out.println("Database setup");
        }

        private void setupClaims() {
            var claimUnverified = create("EXAMPLE-ID-JOB-001", "staff-0001", "alice", new Date(), DetailingStatus.UNVERIFIED,
                    "client-001", "Building WA", null, null, 21000, null);
//            ids.put("unverified", detailingClaimDao.save(claimUnverified).block().getId());
            ids.put("unverified", detailingClaimDao.save(claimUnverified).block());

            var claimStarted = create("EXAMPLE-ID-JOB-002", "staff-0001", "alice", new Date(), DetailingStatus.STARTED,
                    "client-002", "Builders AU", null, null, 21002, null);
//            ids.put("started", detailingClaimDao.save(claimStarted).block().getId());
            ids.put("started", detailingClaimDao.save(claimStarted).block());
            System.out.println("Started ID: " + ids.get("started"));

            var claimStarted2 = create("EXAMPLE-ID-JOB-210", "staff-0002", "bob", new Date(), DetailingStatus.STARTED,
                    "client-002", "Builders AU", null, null, 22002, null);
//            ids.put("started2", detailingClaimDao.save(claimStarted2).block().getId());
            ids.put("started2", detailingClaimDao.save(claimStarted2).block());
            System.out.println("Started2 ID: " + ids.get("started2"));

            var claimStarted3 = create("EXAMPLE-ID-JOB-211", "staff-0002", "bob", new Date(), DetailingStatus.STARTED,
                    "client-002", "Builders AU", null, null, 22003, null);
//            ids.put("started3", detailingClaimDao.save(claimStarted3).block().getId());
            ids.put("started3", detailingClaimDao.save(claimStarted3).block());
            System.out.println("Started3 ID: " + ids.get("started3"));

            var claimStarted4 = create("EXAMPLE-ID-JOB-212", "staff-0002", "bob", new Date(), DetailingStatus.STARTED,
                    "client-002", "Builders AU", null, null, 22004, null);
//            ids.put("started4", detailingClaimDao.save(claimStarted4).block().getId());
            ids.put("started4", detailingClaimDao.save(claimStarted4).block());
            System.out.println("Started4 ID: " + ids.get("started4"));

            var claimPaused = create("EXAMPLE-ID-JOB-003", "staff-0002", "bob", new Date(), DetailingStatus.PAUSED,
                    "client-001", "Building WA", null, null, 21003, null);
//            ids.put("paused", detailingClaimDao.save(claimPaused).block().getId());
//            ids.put("paused", detailingClaimDao.save(claimPaused).block().getJobId());
            ids.put("paused", detailingClaimDao.save(claimPaused).block());

            var claimCancelled = create("EXAMPLE-ID-JOB-004", "staff-0002", "bob", new Date(), DetailingStatus.CANCELLED,
                    "client-002", "Builders AU", null, null, 21004, null);
//            ids.put("cancelled", detailingClaimDao.save(claimCancelled).block().getId());
            ids.put("cancelled", detailingClaimDao.save(claimCancelled).block());

            var claimCancelled02 = create("EXAMPLE-ID-JOB-002", "staff-0002", "bob", new Date(), DetailingStatus.CANCELLED,
                    "client-002", "Builders AU", null, null, 21002, null);
//            ids.put("cancelled_2", detailingClaimDao.save(claimCancelled02).block().getId());
            ids.put("cancelled_2", detailingClaimDao.save(claimCancelled02).block());

            var claimCompleted = create("EXAMPLE-ID-JOB-005", "staff-0001", "alice", new Date(), COMPLETED,
                    "client-001", "Building WA", null, null, 21005, null);
//            ids.put("completed", detailingClaimDao.save(claimCompleted).block().getId());
            ids.put("completed", detailingClaimDao.save(claimCompleted).block());

            assertEquals(9, ids.size());
        }

        private DetailingClaimEntity create(String jobId,
                                            String staffId,
                                            String staffUsername,
                                            Date claimedAt,
                                            DetailingStatus status,
                                            String clientId,
                                            String clientName,
                                            String engineerId,
                                            String engineerName,
                                            Integer floortechJobNumber,
                                            Map<String, FileSetEntity> fileSets) {

            var entity = new DetailingClaimEntity();
            entity.setJobId(jobId);
            entity.setClaimedByStaffId(staffId);
            entity.setClaimedByStaffUsername(staffUsername);
            entity.setCreatedDate(claimedAt);
            entity.setStatus(status);
            entity.setJobClientId(clientId);
            entity.setJobEngineerId(engineerId);
            entity.setJobClientName(clientName);
            entity.setJobEngineerName(engineerName);
            entity.setFloortechJobNumber(floortechJobNumber);
            entity.setFileSets(fileSets);
            return entity;
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustCancelStartedJob() {
            var checkMono = detailingClaimService.getAllActiveClaims(PageRequest.of(0, 10));
            StepVerifier.create(checkMono)
                    .expectNextCount(4)
                    .verifyComplete();

            var detailingClaimEntity = ids.get("started");
            var jobId = detailingClaimEntity.getJobId();
            var claimedByStaffUsername = detailingClaimEntity.getClaimedByStaffUsername();
            System.out.println("STARTED JOB ID = " + jobId);
            var resultMono = detailingClaimService.releaseDetailingClaim(claimedByStaffUsername, jobId);
            StepVerifier.create(resultMono)
                    .consumeNextWith(getDetailingClaimDto -> {
                        assertEquals(DetailingStatus.CANCELLED, getDetailingClaimDto.status());
                    })
                    .verifyComplete();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustCancelPausedJob() {
            var detailingClaimEntity = ids.get("paused");
            var jobId = detailingClaimEntity.getJobId();
            var claimedByStaffUsername = detailingClaimEntity.getClaimedByStaffUsername();
            var claimedAt = detailingClaimEntity.getCreatedDate();
            var all = detailingClaimDao.findAll().collectList().block();
            var filtered = all.stream().filter(d -> d.getJobId().equals(jobId)).toList();
            filtered.forEach(d -> System.out.printf("LOOKED UP FROM DB: %s %s %s \n", d.getJobId(), d.getClaimedByStaffUsername(), d.getStatus()));
            assertEquals(1, filtered.size());

            var pausedList = detailingClaimDao.findByCompoundIdAndStatus(jobId, claimedByStaffUsername, PAUSED).collectList().block();
            assertEquals(1, pausedList.size());
            assertEquals(claimedAt, pausedList.get(0).getCreatedDate());
            var p = pausedList.get(0);
            var sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ssZ");
            System.out.printf("Paused claim looked up with compound index: %s %s %s \n", p.getId(), p.getJobId(), sdf.format(p.getCreatedDate()));

            var pausedCompoundLookup = detailingClaimDao.findByCompoundId(jobId, claimedByStaffUsername, claimedAt).block();
            assertNotNull(pausedCompoundLookup);
            System.out.println("Paused claim looked up: " + pausedCompoundLookup.getId() + " - " + pausedCompoundLookup.getJobId());

            var resultMono = detailingClaimService.releaseDetailingClaim(claimedByStaffUsername, jobId);
            StepVerifier.create(resultMono)
                    .consumeNextWith(getDetailingClaimDto -> {
                        assertEquals(DetailingStatus.CANCELLED, getDetailingClaimDto.status());
                    })
                    .verifyComplete();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustResumePausedJob() {
            var detailingClaimEntity = ids.get("paused");
            var jobId = detailingClaimEntity.getJobId();
            var claimedByStaffUsername = detailingClaimEntity.getClaimedByStaffUsername();
            System.out.println("PAUSED JOB ID = " + jobId);
            var resultMono = detailingClaimService.resumeDetailingClaim(claimedByStaffUsername, jobId);
            StepVerifier.create(resultMono)
                    .consumeNextWith(getDetailingClaimDto -> {
                        assertEquals(DetailingStatus.STARTED, getDetailingClaimDto.status());
                    })
                    .verifyComplete();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustNotCompleteStartedJobBecauseNoFileSet() {
            var checkMono = detailingClaimService.getAllActiveClaims(PageRequest.of(0, 10));
            StepVerifier.create(checkMono)
                    .expectNextCount(4)
                    .verifyComplete();

            var detailingClaimEntity = ids.get("started");
            var jobId = detailingClaimEntity.getJobId();
            var claimedByStaffUsername = detailingClaimEntity.getClaimedByStaffUsername();
            System.out.println("STARTED JOB ID = " + jobId);
            var resultMono = detailingClaimService.completeDetailingClaim(claimedByStaffUsername, jobId);
            StepVerifier.create(resultMono)
                    .consumeErrorWith(error -> {
                        System.out.println("Error message: " + error.getMessage());
                        assertEquals(DetailingClaimServiceException.class, error.getClass());
                        assertEquals(true, error.getMessage().toLowerCase().contains("this claim does not have a file set"));
                    })
                    .verify();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustNotCompleteStartedJobBecauseBadId() {
            var checkMono = detailingClaimService.getAllActiveClaims(PageRequest.of(0, 10));
            StepVerifier.create(checkMono)
                    .expectNextCount(4)
                    .verifyComplete();

            var id = ids.get("started");
            System.out.println("STARTED JOB ID = " + id);
            var resultMono = detailingClaimService.completeDetailingClaim("test_user", "bad-claim-id");
            StepVerifier.create(resultMono)
                    .consumeErrorWith(error -> {
                        System.out.println(error.getMessage());
                        assertEquals(DetailingClaimServiceException.class, error.getClass());
                        assertEquals(true, error.getMessage().toLowerCase().contains("could not find claim for id"));
                    })
                    .verify();
        }

        @Test
        @WithMockUser(username = "alice", roles = {"FT_STAFF"}, authorities = {})
        public void mustCompleteStartedJob() {
            var detailingClaimEntity = ids.get("started");
            var jobId = detailingClaimEntity.getJobId();
            var currentUser = detailingClaimEntity.getClaimedByStaffUsername();
            System.out.println("STARTED JOB ID = " + jobId);

            var file = new File(getClass().getClassLoader().getResource("jobfiles.zip").getFile().replaceAll("%20", " "));
            FileInputStream fileData = null;
            try {
                fileData = new FileInputStream(file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            MockMultipartFile mockMultipartFile = null;
            try {
                mockMultipartFile = new MockMultipartFile("jobfiles.zip", fileData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            var checkMono = detailingClaimService.getAllActiveClaims(PageRequest.of(0, 10));
            StepVerifier.create(checkMono)
                    .expectNextCount(4)
                    .verifyComplete();

            var mockFilePart = new MockFilePartForTests("jobfiles.zip", mockMultipartFile);
            var fileSetDto2 = detailingFileService.submitDetailingFilesArchive(currentUser, jobId, Mono.just(mockFilePart), 700300L).block();
            var fileSetDto1 = detailingFileService.submitDetailingFilesArchive(currentUser, jobId, Mono.just(mockFilePart), 700300L).block();
            System.out.println("File Set 1=" + fileSetDto1.id());
            System.out.println("File Set 2=" + fileSetDto2.id());

            var resultMono = detailingClaimService.completeDetailingClaim(currentUser, jobId);
            StepVerifier.create(resultMono)
                    .consumeNextWith(getDetailingClaimDto -> {
                        System.out.println(getDetailingClaimDto.status());
                        assertEquals(DetailingStatus.COMPLETED, getDetailingClaimDto.status());
                        assertEquals(2, getDetailingClaimDto.fileSets().size());
                    })
                    .verifyComplete();

            // TODO: lookup the parent in the database to get clientName and clientId to delete folders created
//            TestHelpers.cleanupFilesFolders(fileStorage.getRoot(), clientName, clientId);
        }


        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustFindClaimsForUser() {
            var paging = PageRequest.of(0, 50);
            var resultMono = detailingClaimService.getAllClaimsByStaffUsernameAndActive("bob", paging);
            StepVerifier.create(resultMono)
                    .consumeNextWith(dto -> System.out.println(dto))
                    .consumeNextWith(dto -> System.out.println(dto))
                    .consumeNextWith(dto -> System.out.println(dto))
                    .verifyComplete();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustFindActiveClaimsForUserCancelled() {
            var paging = PageRequest.of(0, 50);
            var resultMono = detailingClaimService.getAllClaimsByStaffUsernameAndCancelled("bob", paging);
            StepVerifier.create(resultMono)
                    .consumeNextWith(dto -> System.out.println(dto))
                    .consumeNextWith(dto -> System.out.println(dto))
                    .verifyComplete();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustNotFindClaimsForUserCompleted() {
            var paging = PageRequest.of(0, 50);
            var resultMono = detailingClaimService.getAllClaimsByStaffUsernameAndCompleted("non-existent-user", paging);
            StepVerifier.create(resultMono)
                    .verifyComplete();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustFindClaimByJobId() {
            var paging = PageRequest.of(0, 50);
            var resultMono = detailingClaimService.getCurrentActiveClaimByJobId("EXAMPLE-ID-JOB-001");
            StepVerifier.create(resultMono)
                    .consumeNextWith(dto -> System.out.println(dto))
                    .verifyComplete();

            var resultMono2 = detailingClaimService.getCurrentActiveClaimByJobId("EXAMPLE-ID-JOB-002");
            StepVerifier.create(resultMono2)
                    .consumeNextWith(dto -> System.out.println(dto))
                    .verifyComplete();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustNotFindClaimByJobId() {
            var paging = PageRequest.of(0, 50);
            var resultMono = detailingClaimService.getCurrentActiveClaimByJobId("bad-job-id");
            StepVerifier.create(resultMono)
                    .expectError()
                    .verify();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustFindAllClaimsByJobIds() {
            var idList = List.of("EXAMPLE-ID-JOB-001", "EXAMPLE-ID-JOB-002");
            var resultMono = detailingClaimService.getAllClaimsForJobIds(idList);
            StepVerifier.create(resultMono)
                    .consumeNextWith(dto -> System.out.println("Claim found: " + dto))
                    .consumeNextWith(dto -> System.out.println("Claim found: " + dto))
                    .consumeNextWith(dto -> System.out.println("Claim found: " + dto))
                    .verifyComplete();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustNotFindAnyClaimsByJobIds() {
            var idList = List.of("JOB-DOESNT-EXIST");
            var resultMono = detailingClaimService.getAllClaimsForJobIds(idList);
            StepVerifier.create(resultMono)
                    .verifyComplete();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustNotFindClaimByJobIdsTooManyRequested() {
            List<String> idsList = new ArrayList<>();
            for (int i = 0; i < 150; i++) {
                idsList.add("EXAMPLE-ID-JOB-00"+i);
            }
            var resultMono = detailingClaimService.getAllClaimsForJobIds(idsList);
            StepVerifier.create(resultMono)
                    .consumeErrorWith(error -> {
                        System.out.println(error.getMessage());
                        assertEquals(true, error instanceof ConstraintViolationException);
                        assertEquals(true, error.getMessage().toLowerCase().contains("size must be between"));
                    })
                    .verify();
        }
    }

    @Nested
    class ValidationTests {
        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustNotClaimDetailingJobBecauseJobIdNull() {
            var resultMono = detailingClaimService.claimDetailingJob("test_user", null);
            StepVerifier.create(resultMono)
                    .consumeErrorWith(error -> {
                        System.out.println(error.getMessage());
                        assertEquals(true, error instanceof ConstraintViolationException);
                        assertEquals(true, error.getMessage().toLowerCase().contains("must not be null"));
                    })
                    .verify();
        }
        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustNotClaimDetailingJobBecauseJobIdInvalid() {
            var jobId = "x";
            var resultMono = detailingClaimService.claimDetailingJob("test_user", jobId);
            StepVerifier.create(resultMono)
                    .consumeErrorWith(error -> {
                        System.out.println(error.getMessage());
                        assertEquals(true, error instanceof ConstraintViolationException);
                        assertEquals(true, error.getMessage().toLowerCase().contains("size must be between"));
                    })
                    .verify();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustNotGetAllClaimsForJobIdsBecauseTooManyRequested() {
            var jobIdsList = new ArrayList<String>();
            for (int i = 0; i<=100; i++) {
                jobIdsList.add(UUID.randomUUID().toString());
            }
            var resultMono = detailingClaimService.getAllClaimsForJobIds(jobIdsList);
            StepVerifier.create(resultMono)
                    .consumeErrorWith(error -> {
                        System.out.println(error.getMessage());
                        assertEquals(true, error instanceof ConstraintViolationException);
                        assertEquals(true, error.getMessage().toLowerCase().contains("size must be between"));
                    })
                    .verify();
        }
    }
    private SimpleMessageListenerContainer createMockJobServiceMessageListenerContainer(String queueName, Function<JobDataReplyData, String> replyFunc) {
        var container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(rabbitTemplate.getConnectionFactory());
        container.setQueueNames(queueName);
        container.setPrefetchCount(10);
        container.setBatchSize(10);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setConcurrentConsumers(2);
        if (replyFunc != null) {
            container.setMessageListener(new MockJobServiceMessageListener(rabbitTemplate, replyFunc));
        }
        container.start();
        return container;
    }
}
