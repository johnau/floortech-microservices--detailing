package tech.jmcs.floortech.detailing.app.service;

import jakarta.validation.ConstraintViolationException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tech.jmcs.floortech.common.helper.StringHelper;
import tech.jmcs.floortech.detailing.AppConfiguration;
import tech.jmcs.floortech.detailing.app.dto.GetFileSetDto;
import tech.jmcs.floortech.detailing.app.service.exception.DetailingFileServiceException;
import tech.jmcs.floortech.detailing.domain.configs.DetailingStatus;
import tech.jmcs.floortech.detailing.domain.configs.XPath;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaimFacade;
import tech.jmcs.floortech.detailing.domain.repository.DetailingClaimRepository;
import tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRowFacade;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileDataFacade;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFileFacade;
import tech.jmcs.floortech.detailing.app.dto.GetDetailingFileDto;
import tech.jmcs.floortech.detailing.domain.model.fileset.FileSetFacade;
import tech.jmcs.floortech.detailing.domain.repository.FileSetRepository;
import tech.jmcs.floortech.detailing.domain.service.fileprocessing.FileProcessor;
import tech.jmcs.floortech.detailing.domain.service.FileStorageService;
import tech.jmcs.floortech.detailing.domain.service.ZipFileStorageService;
import tech.jmcs.floortech.detailing.infrastructure.IntegrationTestWithoutEurekaClient;
import tech.jmcs.floortech.detailing.infrastructure.persistence.dao.DetailingClaimDao;
import tech.jmcs.floortech.detailing.infrastructure.persistence.dao.FileSetDao;
import tech.jmcs.floortech.detailing.infrastructure.persistence.dao.MongoDBTestContainer;
import tech.jmcs.floortech.detailing.infrastructure.persistence.entity.DetailingClaimEntity;
import tech.jmcs.floortech.detailing.infrastructure.persistence.entity.FileSetEntity;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@SpringBootTest
@DirtiesContext
@Import({AppConfiguration.class})
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@ActiveProfiles("dev")
public class DetailingFileServiceIntegrationTests implements MongoDBTestContainer, IntegrationTestWithoutEurekaClient {
    @Autowired
    private DetailingFileService detailingFileService;
    @Autowired
    private DetailingClaimRepository detailingClaimRepository;
    @Autowired
    private FileSetRepository fileSetRepository;
    @Autowired
    private FileStorageService fileStorage;
    @Autowired
    private ZipFileStorageService zipFileStorage;
    @Autowired
    private FileProcessor fileProcessor;
    @Autowired
    private DetailingClaimDao detailingClaimDao;
    @Autowired
    private FileSetDao fileSetDao;

    @Test
    @WithMockUser(username = "test-user", roles = {"FT_STAFF"}, authorities = {})
    public void mustSubmitZipFile() {
        fileSetDao.deleteAll().block();
        detailingClaimDao.deleteAll().block();

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

        var clientName = "WA Builders";
        var clientId = "CLIENT-00001";

        var mockFilePart = new MockFilePartForTests("jobfiles.zip", mockMultipartFile);
        var currentUser= "test-user";
        var existingEntity = createExampleDetailingClaim("EXAMPLE-JOB-ID-001", clientName, clientId, currentUser);
        var saved = detailingClaimDao.save(existingEntity).block();
        var claimId = saved.getId();
        var jobId = saved.getJobId();
        var resultMono = detailingFileService.submitDetailingFilesArchive(currentUser, jobId, Mono.just(mockFilePart), 10000L);
        StepVerifier.create(resultMono)
                .consumeNextWith(getFileSetDto -> {
                    System.out.println("File set id: " + getFileSetDto.id());
                    var detailingFiles = getFileSetDto.files();
                    assertEquals(5, detailingFiles.size());
                    assertEquals(true, detailingFiles.stream().filter(x -> x.filePath().path().contains("Beam Listing.txt")).findAny().isPresent());
                    assertEquals(true, detailingFiles.stream().filter(x -> x.filePath().path().contains("Builder.pdf")).findAny().isPresent());
                    assertEquals(true, detailingFiles.stream().filter(x -> x.filePath().path().contains("Steel Cutting List.pdf")).findAny().isPresent());
                    assertEquals(true, detailingFiles.stream().filter(x -> x.filePath().path().contains("260MM TRUSS LISTING.txt")).findAny().isPresent());
                    assertEquals(true, detailingFiles.stream().filter(x -> x.filePath().path().contains("SHEETS.txt")).findAny().isPresent());
                })
                .verifyComplete();

        cleanupFilesFolders(clientName, clientId);
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"FT_STAFF"}, authorities = {})
    public void mustSubmitZipFileAndAddToExisting() {
        fileSetDao.deleteAll().block();
        detailingClaimDao.deleteAll().block();

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

        var clientName = "WA Builders";
        var clientId = "CLIENT-00001";

        var mockFilePart = new MockFilePartForTests("jobfiles.zip", mockMultipartFile);
        var currentUser= "test-user";
        var uuid = UUID.randomUUID().toString();
        var existingEntity = createExampleDetailingClaim("EXAMPLE-JOB-ID-001", clientName, clientId, currentUser);
        var fileSetEntity = new FileSetEntity();
        fileSetEntity.setId(uuid);
        fileSetEntity.setLabel("FileSet001");
        fileSetEntity.setCreatedDate(new Date());
        fileSetEntity.setZipArchiveXPath(XPath.relative("\\WA_Builders_CLIENT-00001\\21000\\xxxxxxxxxxx\\jobfiles.zip"));
        existingEntity.setFileSets(Map.of(uuid, fileSetEntity));

        var saved = detailingClaimDao.save(existingEntity).block();
        var claimId = saved.getId();
        var jobId = saved.getJobId();
        var resultMono = detailingFileService.submitDetailingFilesArchive(currentUser, jobId, Mono.just(mockFilePart), 10000L);
        StepVerifier.create(resultMono)
                .consumeNextWith(getFileSetDto -> {
                    assertEquals(5, getFileSetDto.files().size());
                    System.out.println("ID: " + getFileSetDto.id());
                })
                .verifyComplete();

        var claim = detailingClaimDao.findById(claimId).block();
        assertEquals(2, claim.getFileSets().size());

        cleanupFilesFolders(clientName, clientId);
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"FT_STAFF"}, authorities = {})
    public void mustSubmitZipFileAndThenProcess() {
        fileSetDao.deleteAll().block();
        detailingClaimDao.deleteAll().block();

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

        var clientName = "WA Builders";
        var clientId = "CLIENT-00001";

        var mockFilePart = new MockFilePartForTests("jobfiles.zip", mockMultipartFile);
        var currentUser= "test-user";
        var jobId = "JOB-0000001";
        var existingEntity = createExampleDetailingClaim(jobId, clientName, clientId, currentUser);
        var saved = detailingClaimDao.save(existingEntity).block();
//        var claimId = saved.getId();
        jobId = saved.getJobId();
        var resultMono = detailingFileService.submitDetailingFilesArchive(currentUser, jobId, Mono.just(mockFilePart), 10000L);

        AtomicReference<String> createdFileSetId = new AtomicReference<>("");
        StepVerifier.create(resultMono)
                .consumeNextWith(getFileSetDto -> {
                    System.out.println("File set id: " + getFileSetDto.id());
                    createdFileSetId.set(getFileSetDto.id());
                    var detailingFiles = getFileSetDto.files();
                    assertEquals(5, detailingFiles.size());
                    assertEquals(true, detailingFiles.stream().filter(x -> x.filePath().path().contains("Beam Listing.txt")).findAny().isPresent());
                    assertEquals(true, detailingFiles.stream().filter(x -> x.filePath().path().contains("Builder.pdf")).findAny().isPresent());
                    assertEquals(true, detailingFiles.stream().filter(x -> x.filePath().path().contains("Steel Cutting List.pdf")).findAny().isPresent());
                    assertEquals(true, detailingFiles.stream().filter(x -> x.filePath().path().contains("260MM TRUSS LISTING.txt")).findAny().isPresent());
                    assertEquals(true, detailingFiles.stream().filter(x -> x.filePath().path().contains("SHEETS.txt")).findAny().isPresent());
                })
                .verifyComplete();

        System.out.println("Created id in atmoic ref: " + createdFileSetId.get());
        assertNotEquals(null, createdFileSetId.get());
        assertNotEquals("", createdFileSetId.get());


        var processMono = detailingFileService.processFileSet(currentUser, jobId, createdFileSetId.get());
        StepVerifier.create(processMono)
                .consumeNextWith(getFileSetDto -> {
                    assertEquals(5, getFileSetDto.files().size());
                    var detailingFiles = getFileSetDto.files();
                    detailingFiles.forEach(getDetailingFileDto -> {
                        var filename = getDetailingFileDto.filename();
                        if (filename.contains("260MM TRUSS LISTING.txt")) {
                            assertEquals("text/plain", getDetailingFileDto.mimetype());
                            assertEquals("txt", getDetailingFileDto.extension());
                            assertNotEquals(null, getDetailingFileDto.fileData());
                            assertEquals(true, getDetailingFileDto.fileData().id() != null && !getDetailingFileDto.fileData().id().isEmpty());
                            var extractedData = getDetailingFileDto.fileData().extractedData();
                            System.out.println("Extracted Data Size: " + extractedData.size());
                            assertEquals(28, extractedData.size());
                        } else if (filename.contains("Beam Listing.txt")) {
                            assertEquals("text/plain", getDetailingFileDto.mimetype());
                            assertEquals("txt", getDetailingFileDto.extension());
                            assertNotEquals(null, getDetailingFileDto.fileData());
                            assertEquals(true, getDetailingFileDto.fileData().id() != null && !getDetailingFileDto.fileData().id().isEmpty());
                            var extractedData = getDetailingFileDto.fileData().extractedData();
                            System.out.println("Extracted Data Size: " + extractedData.size());
                            assertEquals(19, extractedData.size());
                        } else if (filename.contains("SHEETS.txt")) {
                            assertEquals("text/plain", getDetailingFileDto.mimetype());
                            assertEquals("txt", getDetailingFileDto.extension());
                            assertNotEquals(null, getDetailingFileDto.fileData());
                            assertEquals(true, getDetailingFileDto.fileData().id() != null && !getDetailingFileDto.fileData().id().isEmpty());
                            var extractedData = getDetailingFileDto.fileData().extractedData();
                            System.out.println("Extracted Data Size: " + extractedData.size());
                            assertEquals(9, extractedData.size());
                        } else if (filename.contains("Builder.pdf")) {
                            assertEquals("application/pdf", getDetailingFileDto.mimetype());
                            assertEquals("pdf", getDetailingFileDto.extension());
                            assertEquals(null, getDetailingFileDto.fileData());
                        } else if (filename.contains("Steel Cutting List.pdf")) {
                            assertEquals("application/pdf", getDetailingFileDto.mimetype());
                            assertEquals("pdf", getDetailingFileDto.extension());
                            assertEquals(null, getDetailingFileDto.fileData());
                        }
                        printDetailingFile(getDetailingFileDto);
                    });
                })
                .verifyComplete();

        cleanupFilesFolders(clientName, clientId);

        var claimsList = detailingClaimDao.findByJobId(jobId).collectList().block();
        assertEquals(1, claimsList.size());
        var claimEntity = claimsList.get(0);
        var fileSets = claimEntity.getFileSets();
        assertEquals(1, fileSets.size());
        fileSets.forEach((id, fileSetEntity) -> {
            System.out.printf("File set id='%s', label='%s' \n", fileSetEntity.getId(), fileSetEntity.getLabel());
        });
        var fileSetEntity = fileSets.entrySet().stream().findFirst().get().getValue();
        var containedFiles = fileSetEntity.getFiles();
        assertEquals(5, containedFiles.size());
        containedFiles.forEach(detailingFileEntity -> {
            var isProcessed = detailingFileEntity.isProcessed();
            var filename = detailingFileEntity.getFilename();
            System.out.printf("Detailing File; id='%s', isProcessed='%s' \n", filename, isProcessed);
        });

        var domainClaim = claimEntity.toDomainObject();
        printDetailingClaimInfo(domainClaim);
    }

    private DetailingClaimEntity createExampleDetailingClaim(String jobId, String clientName, String clientId, String currentUser) {
        var existingEntity = new DetailingClaimEntity();
        existingEntity.setJobId(jobId);
        existingEntity.setFloortechJobNumber(21000);
        existingEntity.setJobClientName(clientName);
        existingEntity.setJobClientId(clientId);
        existingEntity.setJobEngineerName("WA Engineers");
        existingEntity.setJobEngineerId("ENGINEER-00001");
        existingEntity.setStatus(DetailingStatus.STARTED);
        existingEntity.setClaimedByStaffId("STAFF-00001");
        existingEntity.setClaimedByStaffUsername(currentUser);

        return existingEntity;
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"FT_STAFF"}, authorities = {})
    public void mustGetFileSets() {
        fileSetDao.deleteAll().block();
        detailingClaimDao.deleteAll().block();

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

        var jobId = "JOB-0000001";
        var clientName = "WA Builders";
        var clientId = "CLIENT-00001";
        var currentUser= "test-user";
        var uuid = UUID.randomUUID().toString();
        var existingEntity = createExampleDetailingClaim(jobId, clientName, clientId, currentUser);
        var saved = detailingClaimDao.save(existingEntity).block();
        var createdDate = saved.getCreatedDate();
        var claimId = saved.getId();
        var mockFilePart = new MockFilePartForTests("jobfiles.zip", mockMultipartFile);

        // submit two FileSets to the claim
        var fileSetDto1 = detailingFileService.submitDetailingFilesArchive(currentUser, jobId, Mono.just(mockFilePart), 700300L).block();
        var fileSetDto2 = detailingFileService.submitDetailingFilesArchive(currentUser, jobId, Mono.just(mockFilePart), 700300L).block();

        // retrieve fileset list
        var resultMono = detailingFileService.getFileSetListForJobClaim(jobId, currentUser, createdDate);
        StepVerifier.create(resultMono)
                .consumeNextWith(getFileSetDto -> {
                    System.out.println("ID: " + getFileSetDto.id());
                })
                .consumeNextWith(getFileSetDto -> {
                    System.out.println("ID: " + getFileSetDto.id());
                })
                .verifyComplete();

        cleanupFilesFolders(clientName, clientId);
    }

    @Test
    @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
    public void mustGetFileSetAsZip() {
        fileSetDao.deleteAll().block();
        detailingClaimDao.deleteAll().block();

        var file = new File(getClass().getClassLoader().getResource("jobfiles.zip").getFile().replaceAll("%20", " "));
        FileInputStream fileData = null;
        try {
            fileData = new FileInputStream(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        var file2 = new File(getClass().getClassLoader().getResource("jobfiles2.zip").getFile().replaceAll("%20", " "));
        FileInputStream fileData2 = null;
        try {
            fileData2 = new FileInputStream(file2);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        MockMultipartFile mockMultipartFile = null;
        try {
            mockMultipartFile = new MockMultipartFile("jobfiles.zip", fileData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MockMultipartFile mockMultipartFile2 = null;
        try {
            mockMultipartFile2 = new MockMultipartFile("jobfiles2.zip", fileData2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var jobId = "JOB-0000001";
        var clientName = "WA Builders";
        var clientId = "CLIENT-00001";
        var currentUser= "test-user";
        var uuid = UUID.randomUUID().toString();
        var existingEntity = createExampleDetailingClaim(jobId, clientName, clientId, currentUser);
        var saved = detailingClaimDao.save(existingEntity).block();
        var claimedDate = saved.getCreatedDate();
        var claimId = saved.getId();
        var mockFilePart = new MockFilePartForTests("jobfiles.zip", mockMultipartFile);
        var mockFilePart2 = new MockFilePartForTests("jobfiles2.zip", mockMultipartFile2);

        // submit two FileSets to the claim
        var fileSetDto1 = detailingFileService.submitDetailingFilesArchive(currentUser, jobId, Mono.just(mockFilePart), 700300L).block();
        var fileSetDto2 = detailingFileService.submitDetailingFilesArchive(currentUser, jobId, Mono.just(mockFilePart2), 700300L).block();

        var detailingClaim = detailingClaimDao.findByJobId(jobId).blockFirst();
        assertNotEquals(null, detailingClaim);
        var fileSets = detailingClaim.getFileSets();
        assertEquals(2, fileSets.size());
        fileSets.forEach((id, set) -> {
            System.out.println("Set id: " + set.getId() + " | " + set.getZipArchiveXPath().path());
        });

        var resultMono = detailingFileService.getFileSetAsZip(currentUser, jobId, claimedDate, fileSetDto1.id());
        StepVerifier.create(resultMono)
                .expectNextCount(171)
                .verifyComplete();

        var resultMono2 = detailingFileService.getFileSetAsZip(currentUser, jobId, claimedDate, fileSetDto2.id());
        StepVerifier.create(resultMono2)
                .expectNextCount(1)
                .verifyComplete();

        cleanupFilesFolders(clientName, clientId);
    }

    @Test
    @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
    public void mustNotGetFileSetAsZipBecauseFileSetIdDoesNotExist() {
        fileSetDao.deleteAll().block();
        detailingClaimDao.deleteAll().block();

        var jobId = "JOB-0000001";
        var clientName = "WA Builders";
        var clientId = "CLIENT-00001";
        var currentUser = "test-user";
        var uuid = UUID.randomUUID().toString();
        var existingEntity = createExampleDetailingClaim(jobId, clientName, clientId, currentUser);
        var saved = detailingClaimDao.save(existingEntity).block();
        var claimDate = saved.getCreatedDate();
        var claimId = saved.getId();
        System.out.println("Claim id: " + claimId);

        var resultMono = detailingFileService.getFileSetAsZip(currentUser, jobId, claimDate, "id that doesnt exist");
        StepVerifier.create(resultMono)
                .consumeErrorWith(error -> {
                    System.out.println("Error message=" + error.getMessage() + "|" + error.getClass());
                    assertEquals(true, error.getMessage().toLowerCase().contains("could not find fileset with id"));
                    assertEquals(DetailingFileServiceException.class, error.getClass());
                })
                .verify();
    }

    @Test
    @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
    public void mustNotGetFileSetAsZipBecauseClaimIdDoesNotExist() {
        fileSetDao.deleteAll().block();
        detailingClaimDao.deleteAll().block();

        var jobId = "JOB-0000001";
        var clientName = "WA Builders";
        var clientId = "CLIENT-00001";
        var currentUser= "test-user";
        var uuid = UUID.randomUUID().toString();
        var existingEntity = createExampleDetailingClaim(jobId, clientName, clientId, currentUser);
        var saved = detailingClaimDao.save(existingEntity).block();
        var claimDate = saved.getCreatedDate();
        var claimId = saved.getId();

        var resultMono = detailingFileService.getFileSetAsZip(currentUser, "BAD JOB ID", claimDate, "doesn't matter, not reached");
        StepVerifier.create(resultMono)
                .consumeErrorWith(error -> {
                    System.out.println("Error message=" + error.getMessage());
                    assertEquals(true, error.getMessage().toLowerCase().contains("no claim exists for id"));
                    assertEquals(DetailingFileServiceException.class, error.getClass());
                })
                .verify();
    }

    @Nested
    public class ValidationTests {
        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustNotSubmitDetailingFilesArchiveBecauseValidationFailure() {
            var filePart = Mockito.mock(FilePart.class);

            Mono<GetFileSetDto> resultMono = detailingFileService.submitDetailingFilesArchive(null, "CLAIM-0001", Mono.just(filePart), 1000*1000*101L);
            StepVerifier.create(resultMono)
                    .consumeErrorWith(error -> {
                        assertEquals(ConstraintViolationException.class, error.getClass());
                        assertEquals(true, error.getMessage().contains("must not be null"));
                    })
                    .verify();
        }

        @Test
        @WithMockUser(username = "test_user", roles = {"FT_STAFF"}, authorities = {})
        public void mustNotSubmitDetailingFilesArchiveBecauseValidationFailureContentLength() {
            String user = "test_user";
            var filePart = Mockito.mock(FilePart.class);

            Mono<GetFileSetDto> resultMono = detailingFileService.submitDetailingFilesArchive(user, "CLAIM-0001", Mono.just(filePart), 1000*1000*201L);
            StepVerifier.create(resultMono)
                    .consumeErrorWith(error -> {
                        System.out.println(error.getMessage());
                        assertEquals(true, error instanceof ConstraintViolationException);
                        assertEquals(true, error.getMessage().toLowerCase().contains("must be less than or equal to"));
                    })
                    .verify();
        }
    }

    private void printDetailingFile(GetDetailingFileDto getDetailingFileDto) {
        var dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

        var filePath = getDetailingFileDto.filePath();
        var createdDate = getDetailingFileDto.creationDate();
        var _fileData = getDetailingFileDto.fileData();

        System.out.println("File path is : " + filePath.path());
        System.out.println("\t-- Created: " + dateFormat.format(createdDate));
        if (_fileData != null) {
            var fdId = _fileData.id();
            var lines = _fileData.lines();
            var exData = _fileData.extractedData();
            System.out.println("\t\t || File Data ID: " + fdId);
            if (lines != null) {
                System.out.println("\t\t || File lines:");
                for (int i = 0; i < lines.size(); i++) {
                    System.out.println("\t\t\t " + lines.get(i));
                }
            } else {
                System.out.println("Lines is null");
            }
            if (exData != null) {
                System.out.println("\t\t || Extracted Data:");
                exData.forEach((id, data) -> {
                    System.out.println("\t\t\t " + id);
                    System.out.println("\t\t\t " + data);
                });
            }
        }
    }

    private void printDetailingClaimInfo(DetailingClaim detailingClaim) {
//        var id = DetailingClaimFacade.toClaimId.apply(detailingClaim);
        var jobId = DetailingClaimFacade.toJobId.apply(detailingClaim);
        var fileSets = DetailingClaimFacade.toFileSets.apply(detailingClaim);
        var dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        fileSets.forEach((fileSetId, fileSet) -> {
            var createdDate = FileSetFacade.toCreatedDate.apply(fileSet);
            System.out.println("File set id: " + fileSetId + " : " + dateFormatter.format(createdDate));
            var archivePath = FileSetFacade.toZipArchivePath.apply(fileSet);
            var label = FileSetFacade.toLabel.apply(fileSet);

            System.out.printf("Archive Path='%s', label='%s', \n", archivePath.toString(), label);

            var fileList = FileSetFacade.toFiles.apply(fileSet);
            fileList.forEach(detailingFile -> {
                var filePath = DetailingFileFacade.toPath.apply(detailingFile);
                var extension = DetailingFileFacade.toExtension.apply(detailingFile);
                var mimeType = DetailingFileFacade.toMimeType.apply(detailingFile);
                var filename = DetailingFileFacade.toFilename.apply(detailingFile);
                var fileSize = DetailingFileFacade.toFileSize.apply(detailingFile);
                var fileLabel = DetailingFileFacade.toLabel.apply(detailingFile);
                var fileCreationDate = DetailingFileFacade.toCreationDate.apply(detailingFile);
                var fileData = DetailingFileFacade.toFileData.apply(detailingFile);

                System.out.printf("File path='%s'\n", filePath.path());
                System.out.printf("Extension='%s'\n", extension);
                System.out.printf("MimeType='%s'\n", mimeType);
                System.out.printf("File name='%s'\n", filename);
                System.out.printf("File size='%s'\n", fileSize);
                System.out.printf("File label='%s'\n", fileLabel);
                System.out.printf("Creation date='%s'\n", dateFormatter.format(fileCreationDate));

                if (fileData == null) {
                    System.out.println("!!!!!!!!!!!!!! File data is null for: " + filename);
                    return;
                }

                var fileDataId = FileDataFacade.toFileDataId.apply(fileData);
//                var lines = FileDataUtils.toLines.apply(fileData);
                var extractedData = FileDataFacade.toExtractedData.apply(fileData);

                System.out.println("File Data ----------");
                System.out.println("ID: " + fileDataId);
//                System.out.println("Lines count=" + lines.size());
                System.out.println("Extracted Data:::");
                extractedData.forEach((exDataId, rowData) -> {
                    System.out.printf("ID=%s, ROWDATA=%s", exDataId, rowData);
                    var rowId = ExtractedDataRowFacade.toRowId.apply(rowData);
                    var rowNum = ExtractedDataRowFacade.toRow.apply(rowData);
                    var itemId = ExtractedDataRowFacade.toItemId.apply(rowData);
                    var dataMap = ExtractedDataRowFacade.toData.apply(rowData);
                    System.out.printf("Row ID='%s', Row Num='%s', Item Id='%s' \n", rowId, rowNum, itemId);
                    dataMap.forEach((key, value) -> {
                        System.out.printf("Key='%s', Value='%s'\n", key, value);
                    });
                });
            });
        });
    }

    private void cleanupFilesFolders(String clientName, String clientId) {
        var rootPath = fileStorage.getRoot();
        System.out.println("Root storage path: " + rootPath);
        var toDelete = Paths.get(rootPath.toString(), StringHelper.stripInvalidPathCharacters(clientName) + "_" + StringHelper.stripInvalidPathCharacters(clientId));
        System.out.println("To delete folder path: " + toDelete);
        try {
            FileUtils.deleteDirectory(toDelete.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
