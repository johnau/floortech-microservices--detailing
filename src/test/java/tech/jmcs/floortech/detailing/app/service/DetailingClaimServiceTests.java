package tech.jmcs.floortech.detailing.app.service;

import com.rabbitmq.client.Delivery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tech.jmcs.floortech.common.dto.AddressDto;
import tech.jmcs.floortech.common.dto.BuilderClientDto;
import tech.jmcs.floortech.common.dto.EngineerDto;
import tech.jmcs.floortech.common.dto.FloortechJobDto;
import tech.jmcs.floortech.detailing.app.dto.GetDetailingClaimDto;
import tech.jmcs.floortech.detailing.app.dto.mapper.DetailingClaimDtoMapper;
import tech.jmcs.floortech.detailing.app.service.exception.DetailingClaimServiceException;
import tech.jmcs.floortech.detailing.domain.configs.DetailingStatus;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaimFacade;
import tech.jmcs.floortech.detailing.domain.repository.DetailingClaimRepository;
import tech.jmcs.floortech.detailing.domain.service.FloortechJobDataService;
import tech.jmcs.floortech.detailing.domain.service.RemoteLoggingService;
import tech.jmcs.floortech.detailing.infrastructure.messaging.RpcMessageSender;
import tech.jmcs.floortech.detailing.infrastructure.messaging.helper.FloortechJobDtoConverter;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static tech.jmcs.floortech.detailing.domain.configs.DetailingStatus.PAUSED;
import static tech.jmcs.floortech.detailing.domain.configs.DetailingStatus.STARTED;
import static tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaimFacade.*;

@ExtendWith(SpringExtension.class)
public class DetailingClaimServiceTests {
    @InjectMocks
    private DetailingClaimService detailingClaimService;
    @Mock
    private DetailingClaimRepository detailingClaimRepository;
    @Mock
    private RpcMessageSender rpcMessageSender;
    @Mock
    private FloortechJobDtoConverter floortechJobDtoConverter;
    @Mock
    private RemoteLoggingService logMessageSender;
    @Mock
    private DetailingClaimDtoMapper detailingClaimDtoMapper;
    @Mock
    private FloortechJobDataService floortechJobDataService;

    @Test
    public void shouldNotClaimDetailingJobBecauseExisting() {
        String jobId = "AAAAAAAAAAAAA";

        DetailingClaim existingUnverifiedClaim = DetailingClaim.builder(jobId, "user1")
                .status(DetailingStatus.UNVERIFIED)
                .build();

         var savedUnverifiedJobClaim = DetailingClaim.builder(jobId, "")
                .claimedByStaffId("")
                .claimedAt(new Date())
                .status(DetailingStatus.UNVERIFIED)
                .build();

        Mockito.when(detailingClaimRepository.findByJobIdAndClaimed(jobId)).thenReturn(Mono.just(existingUnverifiedClaim));
        Mockito.when(detailingClaimRepository.save(any(DetailingClaim.class))).thenReturn(Mono.just(savedUnverifiedJobClaim));

        var resultMono = detailingClaimService.claimDetailingJob("test_user", jobId);
        StepVerifier.create(resultMono)
                .consumeErrorWith(error -> {
                    System.out.println("Error: " + error.getMessage() + " | " + error.getClass());
                    assertEquals(true, error.getMessage().toLowerCase().contains("already claimed"));
                    assertEquals(true, error instanceof DetailingClaimServiceException);
                })
                .verify();
    }

    @Test
    public void shouldClaimDetailingJob() {
        var jobId = "AAAAAAAAAAAAA";
        String username = "test_user";
        var mapper = new ObjectMapper();

        var unverifiedJobClaim = DetailingClaim.builder(jobId, "")
                .claimedByStaffId("")
                .claimedAt(new Date())
                .status(DetailingStatus.UNVERIFIED)
                .build();
        var claimId = "XXXXXXXXXXX";
        var savedUnverifiedJobClaim = DetailingClaim.builder(jobId, "")
                .claimedByStaffId("")
                .claimedAt(new Date())
                .status(DetailingStatus.UNVERIFIED)
                .build();

        var addressDto = new AddressDto("Unit 5", "Test Street", "Testburb", "Testville",
                                    "6969", "WA", "AU");
        var clientDto = new BuilderClientDto();
        clientDto.setUuid("CLIENT-000-000-001");
        clientDto.setCompanyName("Building WA");
        var engineerDto = new EngineerDto();
        engineerDto.setUuid("ENGINEER-000-000-001");
        engineerDto.setCompanyName("Engineering WA");
        var jobDto = new FloortechJobDto();
        jobDto.setJobNumber(30001);
        jobDto.setAddress(addressDto);
        jobDto.setClient(clientDto);
        jobDto.setEngineer(engineerDto);

        Delivery delivery;
        String json;
        byte[] jsonBytes;
        try {
            json = mapper.writeValueAsString(jobDto);
            jsonBytes = json.getBytes();
            delivery = new Delivery(null, null, jsonBytes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        var detailingClaimUpdatedWithJobData = DetailingClaim.builder(jobId, username)
                .floortechJobNumber(jobDto.getJobNumber())
                .jobClientId(jobDto.getClient().getUuid())
                .jobClientName(jobDto.getClient().getCompanyName())
                .jobEngineerId(jobDto.getEngineer().getUuid())
                .jobEngineerName(jobDto.getEngineer().getCompanyName())
                .status(DetailingStatus.UNVERIFIED)
                .build();

        var returnDto = new GetDetailingClaimDto(claimId, jobId, null, null, null, null, null, null, null, null, null, null);

        Mockito.when(detailingClaimRepository.findByJobIdAndClaimed(jobId)).thenReturn(Mono.empty());
        Mockito.when(detailingClaimRepository.save(any(DetailingClaim.class))).thenReturn(Mono.just(savedUnverifiedJobClaim));
        Mockito.when(floortechJobDataService.requestDataAndUpdate(any(DetailingClaim.class))).thenReturn(Mono.just(detailingClaimUpdatedWithJobData));
        Mockito.when(floortechJobDtoConverter.convert(jsonBytes)).thenReturn(jobDto);
        Mockito.when(detailingClaimRepository.findById(claimId)).thenReturn(Mono.just(savedUnverifiedJobClaim));
        Mockito.when(detailingClaimDtoMapper.toGetDto(any(DetailingClaim.class))).thenReturn(returnDto);

        var resultMono = detailingClaimService.claimDetailingJob(username, jobId);
        StepVerifier.create(resultMono)
                .consumeNextWith(dto -> {
                    assertEquals(claimId, dto.id());
                    assertEquals(jobId, dto.jobId());
                })
                .verifyComplete();
    }

    @Test
    public void shouldGetDetailingClaim() {
        String claimId = "CLAIM000000000000";
        var jobId = "JOB1000000000";
        var staffId = "CLAIMER01";
        var staffUsername = "Test staff user";
        var clientId = "CLIENT10000000";
        var clientName = "Test Client";
        var engineerId = "ENGINEER1000000000";
        var engineerName = "Test Engineer";
        var claimedAt = new Date();
        Integer floortechJobNumber = 21000;
        var existingJobClaim = DetailingClaim.builder(jobId, staffUsername)
                .claimedByStaffId(staffId)
                .claimedAt(claimedAt)
                .status(STARTED)
                .jobClientId(clientId)
                .jobEngineerId(engineerId)
                .jobClientName(clientName)
                .jobEngineerName(engineerName)
                .floortechJobNumber(floortechJobNumber)
                .fileSets(Map.of())
                .build();
        var existingJobClaimDto = writeGetDto(existingJobClaim);

        Mockito.when(detailingClaimRepository.findClaims(anyString(), anyString())).thenReturn(Flux.empty());
        Mockito.when(detailingClaimRepository.findById(claimId)).thenReturn(Mono.just(existingJobClaim));
        Mockito.when(detailingClaimDtoMapper.toGetDto(existingJobClaim)).thenReturn(existingJobClaimDto);
        var resultMono = detailingClaimService.getDetailingClaim(claimId);
        StepVerifier.create(resultMono)
                .consumeNextWith(dto -> {
                   assertEquals(jobId, dto.jobId());
                   assertEquals(staffId, dto.claimedByStaffId());
                   assertEquals(staffUsername, dto.claimedByStaffUsername());
                   assertEquals(claimedAt, dto.claimedAt());
                   assertEquals(floortechJobNumber, dto.floortechJobNumber());
                   assertEquals(engineerId, dto.jobEngineerId());
                   assertEquals(engineerName, dto.jobEngineerName());
                   assertEquals(clientId, dto.jobClientId());
                   assertEquals(clientName, dto.jobClientName());
                   assertEquals(STARTED, dto.status());
                   assertEquals(0, dto.fileSets().size());
                })
                .verifyComplete();
    }

    @Test
    public void shouldNotGetDetailingClaim() {
        var claimId = "CLAIM000000000000";

        Mockito.when(detailingClaimRepository.findClaims(anyString(), anyString())).thenReturn(Flux.empty());
        Mockito.when(detailingClaimRepository.findById(claimId)).thenReturn(Mono.empty());
        var resultMono = detailingClaimService.getDetailingClaim(claimId);
        StepVerifier.create(resultMono)
                .consumeErrorWith(error -> {
                    assertEquals(true, error.getMessage().toLowerCase().contains("could not find claim with id"));
                    assertEquals(true, error instanceof DetailingClaimServiceException);
                })
                .verify();
    }

    @Test
    public void shouldGetClaims() {
        var detailingClaims = createListOfExampleClaims(10);
        var paging = PageRequest.of(0, 5);
        Mockito.when(detailingClaimRepository.findAllActive(paging)).thenReturn(Flux.fromIterable(detailingClaims).take(5));
        for (DetailingClaim detailingClaim : detailingClaims) {
            Mockito.when(detailingClaimDtoMapper.toGetDto(detailingClaim)).thenReturn(writeGetDto(detailingClaim));
        }

        var resultMono = detailingClaimService.getAllActiveClaims(paging);
        StepVerifier.create(resultMono)
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    public void shouldCancelClaim() {
        var _username = "Test staff user";
        var existingJobClaim = createDefaultExample(STARTED, _username);
        var jobId = toJobId.apply(existingJobClaim);
        var claimedByUsername = toClaimedByStaffUsername.apply(existingJobClaim);
        var cancelledClaim = existingJobClaim.withStatus(DetailingStatus.CANCELLED);
        var cancelledClaimDto = writeGetDto(cancelledClaim);

        Mockito.when(detailingClaimRepository.findActiveClaim(jobId, claimedByUsername)).then((Answer<Mono<DetailingClaim>>) invocationOnMock -> {
            System.out.println("Used the composite ID to lookup claim");
            return Mono.just(existingJobClaim);
        });
        Mockito.when(detailingClaimRepository.findPausedClaim(jobId, claimedByUsername)).then((Answer<Mono<DetailingClaim>>) invocationOnMock -> {
            System.out.println("Used the composite ID to lookup claim");
            return Mono.empty();
        });
        Mockito.when(detailingClaimRepository.save(cancelledClaim)).thenReturn(Mono.just(cancelledClaim));
        Mockito.when(detailingClaimDtoMapper.toGetDto(cancelledClaim)).thenReturn(cancelledClaimDto);

        var resultMono2 = detailingClaimService.releaseDetailingClaim(claimedByUsername, jobId);
        StepVerifier.create(resultMono2)
                .consumeNextWith(dto -> {
                    assertEquals(jobId, dto.jobId());
                    assertEquals(claimedByUsername, dto.claimedByStaffUsername());
                    assertEquals(true, dto.status().equals(DetailingStatus.CANCELLED));
                })
                .verifyComplete();
    }

    @Test
    public void shouldNotCancelClaimBecauseDoesntExist() {
        var jobId = "THIS_JOB_DOESNT_EXIST";
        var username = "Test staff user";
        Mockito.when(detailingClaimRepository.findActiveClaim(jobId, username)).thenReturn(Mono.empty());
        Mockito.when(detailingClaimRepository.findPausedClaim(jobId, username)).thenReturn(Mono.empty());
        var resultMono = detailingClaimService.releaseDetailingClaim(username, jobId);
        StepVerifier.create(resultMono)
                .consumeErrorWith(error -> {
                    assertEquals(true, error instanceof DetailingClaimServiceException);
                    assertEquals(true, error.getMessage().toLowerCase().contains("could not find claim for id"));
                })
                .verify();
    }

    @Test
    public void shouldPauseClaim() {
        var username = "Test staff user";

        var existingJobClaim = createDefaultExample(STARTED, username);
        var jobId = toJobId.apply(existingJobClaim);
        var pausedClaim = existingJobClaim.withStatus(PAUSED);
        var pausedClaimDto = writeGetDto(pausedClaim);

        Mockito.when(detailingClaimRepository.findActiveClaim(jobId, username)).thenReturn(Mono.just(existingJobClaim));
        Mockito.when(detailingClaimRepository.save(pausedClaim)).thenReturn(Mono.just(pausedClaim));
        Mockito.when(detailingClaimDtoMapper.toGetDto(pausedClaim)).thenReturn(pausedClaimDto);
        var resultMono = detailingClaimService.pauseDetailingClaim(username, jobId);
        StepVerifier.create(resultMono)
                .consumeNextWith(dto -> {
                    assertEquals(jobId, dto.jobId());
                    assertEquals(username, dto.claimedByStaffUsername());
                    assertEquals(true, dto.status().equals(PAUSED));
                })
                .verifyComplete();
    }

    @Test
    public void shouldNotPauseClaimBecauseDoesntExist() {
        String jobId = "JOB_DOES_NOT_EXIST";
        var username = "Test staff user";

        Mockito.when(detailingClaimRepository.findActiveClaim(jobId, username)).thenReturn(Mono.empty());
        Mockito.when(detailingClaimRepository.findPausedClaim(jobId, username)).thenReturn(Mono.empty());
        var resultMono = detailingClaimService.pauseDetailingClaim(username, jobId);
        StepVerifier.create(resultMono)
                .consumeErrorWith(error -> {
                    System.out.println("Error msg: " + error.getMessage());
                    assertEquals(true, error instanceof DetailingClaimServiceException);
                    assertEquals(true, error.getMessage().toLowerCase().contains("could not find claim for id"));
                })
                .verify();
    }

    @Test
    public void shouldResumeClaim() {
        var username = "Test staff user";
        var existingJobClaim = createDefaultExample(PAUSED, username);
        var resumedClaim = existingJobClaim.withStatus(STARTED);
        var jobId = toJobId.apply(existingJobClaim);

        Mockito.when(detailingClaimRepository.findPausedClaim(jobId, username)).then((Answer<Mono<DetailingClaim>>) invocationOnMock -> Mono.just(existingJobClaim));
        Mockito.when(detailingClaimRepository.save(resumedClaim)).thenReturn(Mono.just(resumedClaim));
        Mockito.when(detailingClaimDtoMapper.toGetDto(resumedClaim)).thenReturn(writeGetDto(resumedClaim));

        var resultMono2 = detailingClaimService.resumeDetailingClaim(username, jobId);
        StepVerifier.create(resultMono2)
                .consumeNextWith(dto -> {
                    assertEquals(jobId, dto.jobId());
                    assertEquals(username, dto.claimedByStaffUsername());
                    assertEquals(true, dto.status().equals(STARTED));
                })
                .verifyComplete();
    }

    @Test
    public void shouldNotResumeClaimBecauseDoesntExist() {
        String jobId = "JOB_DOES_NOT_EXIST";
        String username = "test user";

        Mockito.when(detailingClaimRepository.findPausedClaim(jobId, username)).thenReturn(Mono.empty());
        var resultMono = detailingClaimService.resumeDetailingClaim(username, jobId);
        StepVerifier.create(resultMono)
                .consumeErrorWith(error -> {
                    System.out.println("Error msg" + error.getMessage());
                    assertEquals(true, error instanceof DetailingClaimServiceException);
                    assertEquals(true, error.getMessage().toLowerCase().contains("could not find claim for id"));
                })
                .verify();
    }

    @Test
    public void shouldGetClaimsByUsername() {
        var username = "testuser";
        var paging = PageRequest.of(0, 2);
        var exampleClaims = createListOfExampleClaims(5, username);
        Mockito.when(detailingClaimRepository.findAllByStaffUsernameAndActive(username, paging)).thenReturn(Flux.fromIterable(exampleClaims).take(2));
        for (DetailingClaim detailingClaim : exampleClaims) {
            Mockito.when(detailingClaimDtoMapper.toGetDto(detailingClaim)).thenReturn(writeGetDto(detailingClaim));
        }
        var resultMono = detailingClaimService.getAllClaimsByStaffUsernameAndActive(username, paging);
        StepVerifier.create(resultMono)
                .consumeNextWith(dto -> assertEquals(true, dto.claimedByStaffUsername().equals(username)))
                .consumeNextWith(dto -> assertEquals(true, dto.claimedByStaffUsername().equals(username)))
                .verifyComplete();
    }

    @Test
    public void shouldNotFindAnyClaimsByUsername() {
        var username = "testuser";
        var paging = PageRequest.of(0, 50);
        Mockito.when(detailingClaimRepository.findAllByStaffUsernameAndActive(username, paging)).thenReturn(Flux.empty());
        Mockito.when(detailingClaimDtoMapper.toGetDto(any(DetailingClaim.class))).thenReturn(null);
        var resultMono = detailingClaimService.getAllClaimsByStaffUsernameAndActive(username, paging);
        StepVerifier.create(resultMono)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldNotFindAnyClaimsByUsername2() {
        var username = "testuser";
        var paging = PageRequest.of(0, 50);
        Mockito.when(detailingClaimRepository.findAllByStaffUsernameAndActive(username, paging)).thenReturn(Flux.empty());
        var resultMono = detailingClaimService.getAllClaimsByStaffUsernameAndActive(username, paging);
        StepVerifier.create(resultMono)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldFindClaimByJobId() {
        var jobId = "JOB-0000001";
        var existingClaim = createDefaultExample(jobId, PAUSED);

        Mockito.when(detailingClaimRepository.findByJobIdAndClaimed(jobId)).thenReturn(Mono.just(existingClaim));
        Mockito.when(detailingClaimDtoMapper.toGetDto(existingClaim)).thenReturn(writeGetDto(existingClaim));

        var resultMono = detailingClaimService.getCurrentActiveClaimByJobId(jobId);
        StepVerifier.create(resultMono)
                .consumeNextWith(dto -> {
                    assertEquals(true, dto.jobId().equals(jobId));
                    assertEquals(true, dto.status().equals(PAUSED));
                })
                .verifyComplete();
    }

    @Test
    public void shouldNotFindClaimsByJobIds() {
        var jobIds = new ArrayList<String>();
        for (int i = 1; i < 11; i++) {
            jobIds.add("JOB-000000" + i);
        }

        Mockito.when(detailingClaimRepository.findByJobIds(jobIds)).thenReturn(Flux.empty());
        var resultMono = detailingClaimService.getAllClaimsForJobIds(jobIds);
        StepVerifier.create(resultMono)
                .expectComplete()
                .verify();
    }

    @Test
    public void shouldNotFindSomeClaimsByJobIds() {
        var jobIds = new ArrayList<String>();
        for (int i = 1; i < 11; i++) {
            jobIds.add("JOB-000000" + i);
        }
        var exampleClaims = createListOfExampleClaims(10);

        Mockito.when(detailingClaimRepository.findByJobIds(jobIds)).thenReturn(Flux.fromIterable(exampleClaims).take(5));
        for (DetailingClaim detailingClaim : exampleClaims) {
            Mockito.when(detailingClaimDtoMapper.toGetDto(detailingClaim)).thenReturn(writeGetDto(detailingClaim));
        }

        var resultMono = detailingClaimService.getAllClaimsForJobIds(jobIds);
        StepVerifier.create(resultMono)
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    public void shouldFindAllClaimsByJobIds() {
        var jobIds = new ArrayList<String>();
        for (int i = 1; i < 11; i++) {
            jobIds.add("JOB-000000" + i);
        }
        var exampleClaims = createListOfExampleClaims(10);
        var exampleJobsDtos = exampleClaims.stream().map(x -> writeGetDto(x)).collect(Collectors.toList());

        Mockito.when(detailingClaimRepository.findByJobIds(jobIds)).thenReturn(Flux.fromIterable(exampleClaims));
        for (DetailingClaim detailingClaim : exampleClaims) {
            Mockito.when(detailingClaimDtoMapper.toGetDto(detailingClaim)).thenReturn(writeGetDto(detailingClaim));
        }

        var resultMono = detailingClaimService.getAllClaimsForJobIds(jobIds);
        StepVerifier.create(resultMono)
                .expectNextCount(10)
                .verifyComplete();
    }

    private GetDetailingClaimDto writeGetDto(DetailingClaim detailingClaim) {
        var jobId = DetailingClaimFacade.toJobId.apply(detailingClaim);
        var jobNumber = DetailingClaimFacade.toJobNumber.apply(detailingClaim);
        var jobClientId = DetailingClaimFacade.toClientId.apply(detailingClaim);
        var jobClientName = DetailingClaimFacade.toClientName.apply(detailingClaim);
        var jobEngineerId = DetailingClaimFacade.toEngineerId.apply(detailingClaim);
        var jobEngineerName = DetailingClaimFacade.toEngineerName.apply(detailingClaim);
        var claimedByStaffUname = DetailingClaimFacade.toClaimedByStaffUsername.apply(detailingClaim);
        var claimedByStaffId = DetailingClaimFacade.toClaimedByStaffUserId.apply(detailingClaim);
        var claimedAt = DetailingClaimFacade.toClaimedAt.apply(detailingClaim);
        var fileSets = DetailingClaimFacade.toFileSets.apply(detailingClaim);
        var status = DetailingClaimFacade.toStatus.apply(detailingClaim);

        return new GetDetailingClaimDto(
                null, jobId, jobNumber,
                jobClientId, jobClientName, jobEngineerId, jobEngineerName,
                claimedByStaffUname, claimedByStaffId, claimedAt, new HashMap(), status
        );
    }

    private DetailingClaim createDefaultExample(DetailingStatus status, String username) {
        var jobId = "JOB1000000000";
        var staffId = "CLAIMER01";
        var staffUsername = username;
        var clientId = "CLIENT10000000";
        var clientName = "Test Client";
        var engineerId = "ENGINEER1000000000";
        var engineerName = "Test Engineer";
        var claimedAt = new Date();
        Integer floortechJobNumber = 21000;
        return DetailingClaim.builder(jobId, staffUsername)
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
    }

    private DetailingClaim createDefaultExample(String jobId, DetailingStatus status) {
        var staffId = "CLAIMER01";
        var staffUsername = "Test staff user";
        var clientId = "CLIENT10000000";
        var clientName = "Test Client";
        var engineerId = "ENGINEER1000000000";
        var engineerName = "Test Engineer";
        var claimedAt = new Date();
        Integer floortechJobNumber = 21000;
        return DetailingClaim.builder(jobId, staffUsername)
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
    }

    private List<DetailingClaim> createListOfExampleClaims(int count) {
        var staffUsername = "Test staff user";
        var staffId = "USER-XXXXX-00001";
        return createListOfExampleClaims(count, staffUsername, staffId);
    }

    private List<DetailingClaim> createListOfExampleClaims(int count, String staffUsername) {
        var staffId = "USER-XXXXX-00001";
        return createListOfExampleClaims(count, staffUsername, staffId);
    }

    private List<DetailingClaim> createListOfExampleClaims(int count, String staffUsername, String staffId) {
        var jobId = "JOB1000000000";

        var clientId = "CLIENT10000000";
        var clientName = "Test Client";
        var engineerId = "ENGINEER1000000000";
        var engineerName = "Test Engineer";
        var claimedAt = new Date();
        Integer floortechJobNumber = 21000;

        List<DetailingClaim> exampleClaimsList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            var existingJobClaim = DetailingClaim.builder(jobId + i, staffUsername)
                    .claimedByStaffId(staffId)
                    .claimedAt(claimedAt)
                    .status(STARTED)
                    .jobClientId(clientId)
                    .jobEngineerId(engineerId)
                    .jobClientName(clientName)
                    .jobEngineerName(engineerName)
                    .floortechJobNumber(floortechJobNumber)
                    .fileSets(Map.of())
                    .build();
            exampleClaimsList.add(existingJobClaim);
        }

        return exampleClaimsList;
    }
}
