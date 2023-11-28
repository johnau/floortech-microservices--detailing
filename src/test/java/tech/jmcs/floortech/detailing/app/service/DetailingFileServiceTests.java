package tech.jmcs.floortech.detailing.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tech.jmcs.floortech.common.helper.ArchiveContents;
import tech.jmcs.floortech.detailing.app.dto.GetDetailingFileDto;
import tech.jmcs.floortech.detailing.app.dto.GetFileDataDto;
import tech.jmcs.floortech.detailing.app.dto.mapper.FileSetDtoMapper;
import tech.jmcs.floortech.detailing.app.service.exception.DetailingFileServiceException;
import tech.jmcs.floortech.detailing.infrastructure.files.storage.exception.FileStorageException;
import tech.jmcs.floortech.detailing.domain.service.exception.ZipFileStorageException;
import tech.jmcs.floortech.detailing.domain.configs.XPath;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFileFacade;
import tech.jmcs.floortech.detailing.domain.repository.DetailingClaimRepository;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;
import tech.jmcs.floortech.detailing.domain.model.fileset.FileSet;
import tech.jmcs.floortech.detailing.app.dto.GetFileSetDto;
import tech.jmcs.floortech.detailing.domain.repository.FileSetRepository;
import tech.jmcs.floortech.detailing.domain.service.RemoteLoggingService;
import tech.jmcs.floortech.detailing.domain.service.fileprocessing.FileProcessor;
import tech.jmcs.floortech.detailing.domain.service.FileStorageService;
import tech.jmcs.floortech.detailing.domain.service.IdGenerator;
import tech.jmcs.floortech.detailing.domain.service.ZipFileStorageService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static tech.jmcs.floortech.detailing.domain.model.fileset.FileSetFacade.*;
import static tech.jmcs.floortech.detailing.domain.model.fileset.FileSetFacade.toCreatedDate;
import static tech.jmcs.floortech.detailing.domain.model.fileset.FileSetFacade.toFileSetId;
import static tech.jmcs.floortech.detailing.domain.model.fileset.FileSetFacade.toLabel;

@ExtendWith(SpringExtension.class)
public class DetailingFileServiceTests {
    @InjectMocks
    private DetailingFileService detailingFileService;
    @Mock
    private DetailingClaimRepository detailingClaimRepository;
    @Mock
    private FileSetRepository fileSetRepository;
    @Mock
    private FileStorageService fileStorage;
    @Mock
    private ZipFileStorageService zipFileStorage;
    @Mock
    private FileProcessor fileProcessor;
    @Mock
    private IdGenerator idGenerator;
    @Mock
    private RemoteLoggingService logMessageSender;
    @Mock
    private FileSetDtoMapper fileSetDtoMapper;

    @Test
    public void mustSubmitDetailingFilesArchive() throws ZipFileStorageException {
        FilePart filePart = Mockito.mock(FilePart.class);
        given(filePart.filename()).willReturn("TestFile.zip");
        given(filePart.transferTo(any(Path.class))).willReturn(Mono.empty());
        var jobId = "21000";
        var staffId = "STAFF-0002";
        var staffUsername = "bob";
        var testUuid = UUID.randomUUID().toString();
        System.out.println("Test uuid: " + testUuid);
        var existingClaim = DetailingClaim.builder(jobId, staffUsername)
//                .id("CLAIM-0001")
                .floortechJobNumber(21000)
                .jobClientName("WA Builders")
                .jobClientId("BUILDER-0001")
                .claimedByStaffId(staffId)
                .claimedByStaffUsername(staffUsername)
                .build();

        var zipPath = Paths.get("D:\\temp\\tests\\ft_uploads\\_ft_detailing2\\a\\b\\c\\file.zip");
        var zipPathRelative = Paths.get("\\a\\b\\c\\file.zip");

        var contents = new ArchiveContents();
        contents.setAbsoluteArchivePath(zipPath);
        contents.setRelativeArchivePath(zipPathRelative);
        contents.setContainedFileRelativePaths(List.of(
                Paths.get("/a/b/c/file1.txt"),
                Paths.get("/a/b/c/file2.txt"),
                Paths.get("/a/b/c/file3.txt"),
                Paths.get("/a/b/c/file4.txt"),
                Paths.get("/a/b/c/file5.txt")
        ));

        List<DetailingFile> detailingFileList = List.of(
                DetailingFile.createUnprocessedDetailingFile(XPath.relative("/a/b/c/file1.txt"), "file1.txt"),
                DetailingFile.createUnprocessedDetailingFile(XPath.relative("/a/b/c/file2.txt"), "file2.txt"),
                DetailingFile.createUnprocessedDetailingFile(XPath.relative("/a/b/c/file3.txt"), "file3.txt"),
                DetailingFile.createUnprocessedDetailingFile(XPath.relative("/a/b/c/file4.txt"), "file4.txt"),
                DetailingFile.createUnprocessedDetailingFile(XPath.relative("/a/b/c/file5.txt"), "file5.txt")
        );
        var newFileSet = FileSet.createNewFileSet(testUuid, XPath.relative(""), detailingFileList);
        var updatedClaim = existingClaim.withFileSets(Map.of(toFileSetId.apply(newFileSet), newFileSet));

        Mockito.when(detailingClaimRepository.findActiveClaim(jobId, staffUsername)).thenReturn(Mono.just(existingClaim));
//        Mockito.when(detailingClaimRepository.findById("CLAIM-0001")).thenReturn(Mono.just(existingClaim));
        Mockito.when(fileStorage.saveToRoot(eq(filePart), anyString(), anyString(), anyString())).thenReturn(Mono.just(zipPath));
        Mockito.when(fileStorage.makeRelative(Paths.get("D:\\temp\\tests\\ft_uploads\\_ft_detailing2\\a\\b\\c\\file.zip"))).thenReturn(Paths.get("a\\b\\c\\file.zip"));
        Mockito.when(zipFileStorage.unzipArchive(any(Path.class), eq(false))).thenReturn(contents);
        Mockito.when(detailingClaimRepository.save(any(DetailingClaim.class))).thenReturn(Mono.just(updatedClaim));
        Mockito.when(idGenerator.generateUuid()).thenReturn(testUuid);
        Mockito.when(fileSetDtoMapper.toGetDto(any(FileSet.class))).thenReturn(writeGetDto(newFileSet));

        var resultMono = detailingFileService.submitDetailingFilesArchive(staffUsername, jobId, Mono.just(filePart), 10000L);
        StepVerifier.create(resultMono)
                .consumeNextWith(getFileSetDto -> {
                    System.out.println(getFileSetDto);
                    assertEquals(5, getFileSetDto.files().size());
                })
                .verifyComplete();
    }

    private GetFileSetDto writeGetDto(FileSet fileSet) {
        var id = toFileSetId.apply(fileSet);
        var label = toLabel.apply(fileSet);
        var files = toFiles.apply(fileSet);
        var jobClaim = toDetailingClaim.apply(fileSet);
        var zipArchivePath = toZipArchivePath.apply(fileSet);
        var createdDate = toCreatedDate.apply(fileSet);

        List<GetDetailingFileDto> fileDtos = List.of();
        if (files != null) {
            fileDtos = files.stream()
                    .map(this::writeGetDFileDto)
                    .collect(Collectors.toList());
        }

//        GetDetailingClaimDto jobClaimDto = null;
//        String jobClaimId = null;
//        if (jobClaim != null) {
//            jobClaimId = toClaimId.apply(jobClaim);
//        }

        Objects.requireNonNull(fileDtos); // keep

        return new GetFileSetDto(
                id, label, fileDtos, null, zipArchivePath, createdDate
        );
    }

    private GetDetailingFileDto writeGetDFileDto(DetailingFile detailingFile) {
        var id = DetailingFileFacade.toFileId.apply(detailingFile);
        var path = DetailingFileFacade.toPath.apply(detailingFile);
        var extension = DetailingFileFacade.toExtension.apply(detailingFile);
        var filename = DetailingFileFacade.toFilename.apply(detailingFile);
        var fileSize = DetailingFileFacade.toFileSize.apply(detailingFile);
        var mimetype = DetailingFileFacade.toMimeType.apply(detailingFile);
        var parentFolder = DetailingFileFacade.toParentFolder.apply(detailingFile);
        var label = DetailingFileFacade.toLabel.apply(detailingFile);
        var creationDate = DetailingFileFacade.toCreationDate.apply(detailingFile);
        var fileData = DetailingFileFacade.toFileData.apply(detailingFile);

        GetFileDataDto getFileDataDto = null;
//        if (fileData != null) {
//            getFileDataDto = fileDataDtoMapper.writeGetDto(fileData);
//        }

        return new GetDetailingFileDto(
                id, path, extension, filename, fileSize, mimetype, parentFolder, label, creationDate,
                getFileDataDto
        );
    }

    @Test
    public void mustNotSubmitDetailingFilesArchiveBecauseWrongUser() {
        var user = "test_user";
        var jobId = "21000";
        var other_username = "STAFF-0002";
        var existingClaim = DetailingClaim.builder("JOB-000001", "other-user")
//                .id("CLAIM-0001")
                .jobId(jobId)
                .claimedByStaffId(other_username)
                .build();
        var filePart = Mockito.mock(FilePart.class);
        given(filePart.filename()).willReturn("TestFile.zip");
        given(filePart.transferTo(any(Path.class))).willReturn(Mono.empty());

        Mockito.when(detailingClaimRepository.findActiveClaim(jobId, user)).thenReturn(Mono.just(existingClaim));
        Mono<GetFileSetDto> resultMono = detailingFileService.submitDetailingFilesArchive(user, jobId, Mono.just(filePart), 10000L);
        StepVerifier.create(resultMono)
                .consumeErrorWith(error -> {
                    assertEquals(true, error instanceof DetailingFileServiceException);
                    assertEquals(true, error.getMessage().toLowerCase().contains("not the current owner of this claim"));
                })
                .verify();
    }

    @Test
    public void mustNotSubmitDetailFilesArchiveBecauseFileError() {
        var jobId = "21000";
        var userId = "STAFF-0002";
        var username = "bob";
        var existingClaim = DetailingClaim.builder("JOB-00001", "test-user")
//                .id("CLAIM-0001")
                .jobId(jobId)
                .floortechJobNumber(21000)
                .claimedByStaffId(userId)
                .claimedByStaffUsername(username)
                .jobClientId("XYXYXYXYXYXYX")
                .jobClientName("A company")
                .build();
        var filePart = Mockito.mock(FilePart.class);
        given(filePart.filename()).willReturn("TestFile.zip");
        given(filePart.transferTo(any(Path.class))).willReturn(Mono.empty());

        Mockito.when(detailingClaimRepository.findActiveClaim(jobId, username)).thenReturn(Mono.just(existingClaim));
//        Mockito.when(detailingClaimRepository.findById("CLAIM-0001")).thenReturn(Mono.just(existingClaim));
        Mockito.when(fileStorage.saveToRoot(eq(filePart), anyString(), anyString(), anyString())).thenReturn(Mono.error(new FileStorageException("")));

        Mono<GetFileSetDto> resultMono = detailingFileService.submitDetailingFilesArchive(username, jobId, Mono.just(filePart), 10000L);
        StepVerifier.create(resultMono)
                .consumeErrorWith(error -> {
                    System.out.println("Error class: " + error.getClass());
                    System.out.println("Error message: " + error.getMessage());
                    assertEquals(true, error instanceof FileStorageException);
                })
                .verify();
    }

//    @Test
//    public void mustSubmitDetailingFilesArchiveAndProcess() {
//        FilePart filePart = Mockito.mock(FilePart.class);
//        given(filePart.filename()).willReturn("TestFile.zip");
//        given(filePart.transferTo(any(Path.class))).willReturn(Mono.empty());
//
//        var existingClaim = DetailingClaim.builder()
//                .id("CLAIM-0001")
//                .floortechJobNumber(21000)
//                .jobClientName("WA Builders")
//                .jobClientId("BUILDER-0001")
//                .claimedByStaffUsername("test_user")
//                .claimedByStaffId("STAFF-0002")
//                .build();
//
//        var archiveContents = new ZipFileStorage.ArchiveContents();
//        archiveContents.setAbsoluteArchivePath(Paths.get("D:\\temp\\tests\\ft_uploads\\_ft_detailing2\\a\\b\\c\\file.zip"));
//        archiveContents.setRelativeArchivePath(Paths.get("a\\b\\c\\file.zip"));
//        archiveContents.setContainedFileAbsolutePaths(List.of(
//                Paths.get("D:\\temp\\tests\\ft_uploads\\_ft_detailing2\\a\\b\\c\\file1.txt"),
//                Paths.get("D:\\temp\\tests\\ft_uploads\\_ft_detailing2\\a\\b\\c\\file2.txt")
//        ));
//        archiveContents.setContainedFileRelativePaths(List.of(
//                Paths.get("a\\b\\c\\file1.txt"),
//                Paths.get("a\\b\\c\\file2.txt")
//        ));
//
//        Mockito.when(detailingClaimRepository.findById("CLAIM-0001")).thenReturn(Mono.just(existingClaim));
//        Mockito.when(zipFileStorage.saveArchiveAndExtract(eq(filePart), anyString(), anyString(), anyString())).thenReturn(Mono.just(archiveContents));
//        Mockito.when(fileStorage.makeRelative(Paths.get("D:\\temp\\tests\\ft_uploads\\_ft_detailing2\\a\\b\\c\\file1.txt"))).thenReturn(Paths.get("a\\b\\c\\file1.txt"));
//        Mockito.when(fileStorage.makeRelative(Paths.get("D:\\temp\\tests\\ft_uploads\\_ft_detailing2\\a\\b\\c\\file2.txt"))).thenReturn(Paths.get("a\\b\\c\\file2.txt"));
//
//        var user = "test_user";
//        Mono<GetFileSetDto> resultMono = detailingFileService.submitAndProcessDetailingFilesArchive(user, "CLAIM-0001", Mono.just(filePart), 10000L);
//        StepVerifier.create(resultMono)
//                .consumeNextWith(dto -> {
//                    assertEquals(2, dto.files().size());
//                })
//                .verifyComplete();
//    }


}
