package tech.jmcs.floortech.detailing.infrastructure.persistence.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tech.jmcs.floortech.common.dto.BuilderClientDto;
import tech.jmcs.floortech.common.dto.EngineerDto;
import tech.jmcs.floortech.common.dto.FloortechJobDto;
import tech.jmcs.floortech.detailing.AppConfiguration;
import tech.jmcs.floortech.detailing.domain.configs.DetailingStatus;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.infrastructure.persistence.entity.DetailingClaimEntity;
import tech.jmcs.floortech.detailing.infrastructure.persistence.entity.FileSetEntity;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static tech.jmcs.floortech.detailing.domain.configs.DetailingStatus.STARTED;

@DataMongoTest
@ExtendWith(SpringExtension.class)
@Import(AppConfiguration.class)
//@ContextConfiguration(classes = {MongoDBTestContainerConfig.class}) // using the MongoDBTestContainer Interface instead
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DetailingClaimDaoIntegrationTest implements MongoDBTestContainer {
    @Autowired
    private DetailingClaimDao detailingClaimDao;

    @Test
    public void shouldCreateDetailingClaim() {
        String claimId = "CLAIM000000000000";
        var jobId = "JOB0000001";
        var status = DetailingStatus.UNVERIFIED;
        var staffId = "CLAIMER01";
        var staffUsername = "Test staff user";
        var clientId = "CLIENT10000000";
        var clientName = "Test Client";
        var engineerId = "ENGINEER1000000000";
        var engineerName = "Test Engineer";
        var claimedAt = new Date();
        Integer floortechJobNumber = 21000;
        var detailingClaim = DetailingClaim.builder(jobId, staffUsername)
//                .id(claimId)
                .claimedByStaffId(staffId)
                .claimedAt(claimedAt)
                .status(status)
                .jobClientId(clientId)
                .jobEngineerId(engineerId)
                .jobClientName(clientName)
                .jobEngineerName(engineerName)
                .floortechJobNumber(floortechJobNumber)
                .fileSets(Map.of())
                .build();
        var entity = DetailingClaimEntity.fromDomainObject(detailingClaim);
        Publisher<DetailingClaimEntity> setup = detailingClaimDao.deleteAll().thenMany(detailingClaimDao.save(entity));
        StepVerifier
                .create(setup)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Nested
    class NestedTests {
        @BeforeEach
        void setupForTests() {
            var step0 = detailingClaimDao.deleteAll();
            var step1 = setupClaims();
            var combined = Mono.from(step0)
                                                    .thenMany(step1);
            StepVerifier
                    .create(combined)
                    .consumeNextWith(System.out::println)
                    .consumeNextWith(System.out::println)
                    .consumeNextWith(System.out::println)
                    .consumeNextWith(System.out::println)
                    .consumeNextWith(System.out::println)
                    .consumeNextWith(System.out::println)
                    .verifyComplete();
        }

        private Publisher<DetailingClaimEntity> setupClaims() {
            var claimUnverified = create("JOB-001", "staff-0001", "alice", new Date(), DetailingStatus.UNVERIFIED,
                    "client-001", "Building WA", null, null, 21000, null);
            var claimStarted = create("JOB-002", "staff-0001", "alice", new Date(), STARTED,
                    "client-002", "Builders AU", null, null, 21002, null);
            var claimPaused = create("JOB-003", "staff-0002", "bob", new Date(), DetailingStatus.PAUSED,
                    "client-001", "Building WA", null, null, 21003, null);
            var claimCancelled = create("JOB-004", "staff-0002", "bob", new Date(), DetailingStatus.CANCELLED,
                    "client-002", "Builders AU", null, null, 21004, null);
            var claimCancelled02 = create("JOB-002", "staff-0002", "bob", new Date(), DetailingStatus.CANCELLED,
                    "client-002", "Builders AU", null, null, 21002, null);
            var claimCompleted = create("JOB-005", "staff-0001", "alice", new Date(), DetailingStatus.COMPLETED,
                    "client-001", "Building WA", null, null, 21005, null);

            return detailingClaimDao.saveAll(List.of(
                    claimUnverified, claimCancelled02, claimStarted, claimPaused, claimCancelled, claimCompleted
            ));
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

            var detailingClaim = new DetailingClaimEntity();
            detailingClaim.setJobId(jobId);
            detailingClaim.setClaimedByStaffId(staffId);
            detailingClaim.setClaimedByStaffUsername(staffUsername);
            detailingClaim.setCreatedDate(claimedAt);
            detailingClaim.setStatus(status);
            detailingClaim.setJobClientId(clientId);
            detailingClaim.setJobEngineerId(engineerId);
            detailingClaim.setJobClientName(clientName);
            detailingClaim.setJobEngineerName(engineerName);
            detailingClaim.setFloortechJobNumber(floortechJobNumber);
            detailingClaim.setFileSets(fileSets);
            return detailingClaim;
        }

        @Test
        public void shouldFindAllClaims() {
            var findAll = detailingClaimDao.findAll();
            StepVerifier.create(findAll)
                    .consumeNextWith(entity -> {
                        System.out.printf("Found claim: %s %s %s \n", entity.getId(), entity.getJobId(), entity.getFloortechJobNumber());
                    })
                    .consumeNextWith(entity -> {
                        System.out.printf("Found claim: %s %s %s \n", entity.getId(), entity.getJobId(), entity.getFloortechJobNumber());
                    })
                    .consumeNextWith(entity -> {
                        System.out.printf("Found claim: %s %s %s \n", entity.getId(), entity.getJobId(), entity.getFloortechJobNumber());
                    })
                    .consumeNextWith(entity -> {
                        System.out.printf("Found claim: %s %s %s \n", entity.getId(), entity.getJobId(), entity.getFloortechJobNumber());
                    })
                    .consumeNextWith(entity -> {
                        System.out.printf("Found claim: %s %s %s \n", entity.getId(), entity.getJobId(), entity.getFloortechJobNumber());
                    })
                    .consumeNextWith(entity -> {
                        System.out.printf("Found claim: %s %s %s \n", entity.getId(), entity.getJobId(), entity.getFloortechJobNumber());
                    })
                    .verifyComplete();
        }

        @Test
        public void shouldNotCreateClaimBecauseExistingActive() {
            var newClaim = create("JOB-002", "staff-0002", "bob", new Date(), DetailingStatus.UNVERIFIED,
                    "client-002", "Builders AU", null, null, 21002, null);

            var saveIfNoOthers = detailingClaimDao.findByJobIdAndStatus("JOB-002", STARTED)
                    .switchIfEmpty(detailingClaimDao.save(newClaim));

            StepVerifier.create(saveIfNoOthers)
                    .consumeNextWith(updated -> {
                        assertEquals(true, updated.getStatus().equals(STARTED));
                        System.out.printf("Found claim: %s %s %s %s\n", updated.getId(), updated.getJobId(), updated.getFloortechJobNumber(), updated.getStatus());
                    })
                    .verifyComplete();
        }

        @Test
        public void shouldCreateVerifiedClaim() {
            var newClaim = create("JOB-111", "staff-0003", "charlie", new Date(), DetailingStatus.UNVERIFIED,
                    null, null, null, null, null, null);

            Mono<DetailingClaimEntity> createMono = detailingClaimDao.save(newClaim)
                    .flatMap(entity -> detailingClaimDao.findById(entity.getId()))
                    .flatMap(entity -> {
                        var client = new BuilderClientDto();
                        client.setUuid("LSKDJF-KLSDJF-SLKDFJ-SLDKFJ");
                        client.setCompanyName("Bob the builder");
                        var engineer = new EngineerDto();
                        engineer.setUuid("ASDF-ASFF-ASFAF-ASFASF");
                        engineer.setCompanyName("Eric the Engineer");
                        var responseData = new FloortechJobDto();
                        responseData.setJobNumber(22000);
                        responseData.setEngineer(engineer);
                        responseData.setClient(client);

//                        var verified = entity.withFloortechJobNumber(responseData.getJobNumber())
//                                .withJobClientId(responseData.getClient().getUuid())
//                                .withJobClientName(responseData.getClient().getCompanyName())
//                                .withJobEngineerId(responseData.getEngineer().getUuid())
//                                .withJobEngineerName(responseData.getEngineer().getCompanyName())
//                                .withStatus(STARTED);
                        entity.setFloortechJobNumber(responseData.getJobNumber());
                        entity.setJobClientId(responseData.getClient().getUuid());
                        entity.setJobClientName(responseData.getClient().getCompanyName());
                        entity.setJobEngineerId(responseData.getEngineer().getUuid());
                        entity.setJobEngineerName(responseData.getEngineer().getCompanyName());
                        entity.setStatus(STARTED);

                        return Mono.just(entity);
                    })
                    .flatMap(entity -> detailingClaimDao.save(entity));

            StepVerifier.create(createMono)
                    .consumeNextWith(updated -> {
                        assertEquals(true, updated.getStatus().equals(STARTED));
                        System.out.println(updated.toString());
                    })
                    .verifyComplete();
        }

        @Test
        public void shouldFindOneActiveClaims() {
            var paging = PageRequest.of(0, 10);
            var findAll = detailingClaimDao.findByStatus(STARTED, paging);
            StepVerifier.create(findAll)
                    .consumeNextWith(entity -> {
                        System.out.printf("Found claim: %s %s %s \n", entity.getId(), entity.getJobId(), entity.getFloortechJobNumber());
                    })
                    .verifyComplete();
        }

        @Test
        public void shouldCancelOneActiveClaim() {
            var paging = PageRequest.of(0, 10);
            var findAll = detailingClaimDao.findByStatus(STARTED, paging)
                    .flatMap(entity -> {
                        var cancelledStatus = DetailingStatus.CANCELLED;
                        entity.setStatus(cancelledStatus);
                        return detailingClaimDao.save(entity);
                    });
            StepVerifier.create(findAll)
                    .consumeNextWith(entity -> {
                        System.out.printf("Found claim: %s %s %s %s \n", entity.getId(), entity.getJobId(), entity.getFloortechJobNumber(), entity.getStatus());
                    })
                    .verifyComplete();
        }

        @Test
        public void shouldUpdateClaimFromUnverifiedToStarted() {
            var jobId = "JOB-001";
            var updateUnverified = detailingClaimDao.findByJobId(jobId)
                    .switchIfEmpty(Mono.error(new Exception("Fail")))
                    .filter(entity -> entity.getStatus().equals(DetailingStatus.UNVERIFIED))
                    .flatMap(entity -> {
                        entity.setStatus(STARTED);
                        return detailingClaimDao.save(entity);
                    });

            StepVerifier.create(updateUnverified)
                    .consumeNextWith(updated -> {
                        assertEquals(true, updated.getStatus().equals(STARTED));
                        assertEquals(true, updated.getFloortechJobNumber().equals(21000));
                        System.out.printf("Found claim: %s %s %s %s\n", updated.getId(), updated.getJobId(), updated.getFloortechJobNumber(), updated.getStatus());
                    })
                    .verifyComplete();
        }

        @Test
        public void shouldFindClaimByStatus() {
            var updateUnverified = detailingClaimDao.findByStatus(DetailingStatus.PAUSED);

            StepVerifier.create(updateUnverified)
                    .consumeNextWith(updated -> {
                        assertEquals(true, updated.getStatus().equals(DetailingStatus.PAUSED));
                        System.out.printf("Found claim: %s %s %s %s\n", updated.getId(), updated.getJobId(), updated.getFloortechJobNumber(), updated.getStatus());
                    })
                    .verifyComplete();
        }
    }

}
