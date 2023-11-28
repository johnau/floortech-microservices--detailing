package tech.jmcs.floortech.detailing.app.service;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import tech.jmcs.floortech.common.auth.IsAuthenticatedAsFloortechInternalUser;
import tech.jmcs.floortech.common.helper.ArchiveContents;
import tech.jmcs.floortech.common.helper.FileHelper;
import tech.jmcs.floortech.detailing.app.dto.mapper.FileSetDtoMapper;
import tech.jmcs.floortech.detailing.domain.service.exception.FileProcessorException;
import tech.jmcs.floortech.detailing.app.service.exception.DetailingFileServiceException;
import tech.jmcs.floortech.detailing.domain.service.exception.ZipFileStorageException;
import tech.jmcs.floortech.detailing.domain.configs.XPath;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaimFacade;
import tech.jmcs.floortech.detailing.domain.model.fileset.FileSet;
import tech.jmcs.floortech.detailing.domain.repository.DetailingClaimRepository;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;
import tech.jmcs.floortech.detailing.app.dto.GetFileSetDto;
import tech.jmcs.floortech.detailing.domain.service.RemoteLoggingService;
import tech.jmcs.floortech.detailing.domain.service.fileprocessing.FileProcessor;
import tech.jmcs.floortech.detailing.domain.service.FileStorageService;
import tech.jmcs.floortech.detailing.domain.service.IdGenerator;
import tech.jmcs.floortech.detailing.domain.service.ZipFileStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaimFacade.*;
import static tech.jmcs.floortech.detailing.domain.model.files.DetailingFileFacade.*;
import static tech.jmcs.floortech.detailing.domain.model.fileset.FileSetFacade.*;

@Service
@Validated
public class DetailingFileService {
    final static Logger log = LoggerFactory.getLogger(DetailingFileService.class);
    private static final long MAX_CONTENT_LENGTH = 100*1000*1000;
    final DetailingClaimRepository detailingClaimRepository;
    final FileStorageService fileStorage;
    final ZipFileStorageService zipFileStorage;
    @Qualifier("archicadDataFileProcessingService")
    final FileProcessor fileProcessor;
    final IdGenerator idGenerator;
    final RemoteLoggingService logMessageSender;
    final FileSetDtoMapper fileSetDtoMapper;
    final Validator validator;

    @Autowired
    public DetailingFileService(DetailingClaimRepository detailingClaimRepository, FileStorageService fileStorage, ZipFileStorageService zipFileStorage, FileProcessor fileProcessor, IdGenerator idGenerator, RemoteLoggingService logMessageSender, FileSetDtoMapper fileSetDtoMapper, Validator validator) {
        this.detailingClaimRepository = detailingClaimRepository;
        this.fileStorage = fileStorage;
        this.zipFileStorage = zipFileStorage;
        this.fileProcessor = fileProcessor;
        this.idGenerator = idGenerator;
        this.logMessageSender = logMessageSender;
        this.fileSetDtoMapper = fileSetDtoMapper;
        this.validator = validator;
    }

    // <editor-fold desc="Webflux Service methods">
    /**
     * Accepts a zip file to be attached to a DetailingClaim
     * The method is only accessible to Internal users
     * TODO: Validate FilePart is zip archive and validate file size
     * @param currentUser
     * @param jobId
     * @param filePartMono
     * @param contentLength
     * @return Dto Mono
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Mono<GetFileSetDto> submitDetailingFilesArchive(@NotNull String currentUser,
                                                           @NotNull @Size(min = 10, max = 100) String jobId,
                                                           @NotNull Mono<FilePart> filePartMono,
                                                           @NotNull @Max(MAX_CONTENT_LENGTH+1000) long contentLength) {
        return getActiveDetailingClaimIfAllowed(jobId, currentUser)
                .onErrorStop()
                .flatMap(detailingClaim -> storeFileMono(filePartMono, detailingClaim))
                .handle(this::unzipArchiveOrError).log()
                .map(this::createFileSetFromFiles)
                .zipWith(getActiveDetailingClaimIfAllowed(jobId, currentUser))
                .flatMap(tuple -> saveFileSetToDetailingClaim(tuple.getT1(), tuple.getT2()))
                .map(fileSetDtoMapper::toGetDto).log()
                        .doOnNext(fileSetDto -> logMessageSender.sendBusinessInfo("Submitted FileSet for detailing claim: ID=" + fileSetDto.jobClaimId() + ", FileSet Label=" + fileSetDto.label() + ", User=" + currentUser));
    }

    /**
     * Attempts to extract scheduling data from files in a file set.
     * The method is only accessible to Internal users
     * @param currentUser
     * @param claimId
     * @param fileSetId
     * @return Dto Mono
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Mono<GetFileSetDto> processFileSet(@NotNull String currentUser, @NotNull @Size(min = 10, max = 100) String claimId, @NotNull @Size(min = 10, max = 100) String fileSetId) {
        var processedMono = getActiveDetailingClaimIfAllowed(claimId, currentUser)
                .map(toFileSets::apply)
                .map(fileSetList -> fileSetList.get(fileSetId))
                .flatMapIterable(toFiles::apply)
                .flatMap(this::preprocessDetailingFile)
                .onErrorComplete()
                .map(this::processDetailingFile).collectList();

        return processedMono
                .zipWith(getActiveDetailingClaimIfAllowed(claimId, currentUser))
                .map(tuple -> addDetailingFilesToDetailingClaimFileSet(fileSetId, tuple.getT1(),tuple.getT2()))
                .flatMap(detailingClaimRepository::save)
                .map(toFileSets::apply)
                .map(fileSets -> fileSets.get(fileSetId))
                .map(fileSetDtoMapper::toGetDto);
    }

    /**
     * Get a list of file information for a specific claim.  (Does not include the actual file)
     * The method is only accessible to Internal users
     * @param jobId
     * @param claimedByUsername
     * @param claimedDate
     * @return Dto Flux
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Flux<GetFileSetDto> getFileSetListForJobClaim(@NotNull @Size(min = 10, max = 100) String jobId,
                                                         @NotNull String claimedByUsername,
                                                         @NotNull Date claimedDate) {
        return detailingClaimRepository.findClaim(jobId, claimedByUsername, claimedDate)
                .map(toFileSets::apply)
                .map(Map::values)
                .flatMapMany(Flux::fromIterable)
                .map(fileSetDtoMapper::toGetDto);
    }

    /**
     * Get a list of all files information for a job. This can include multiple claims.  (Does not include the actual file)
     * The method is only accessible to Internal users
     * @param jobId
     * @return Dto Flux
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Flux<GetFileSetDto> getAllFileSetsForJobId(@NotNull @Size(min = 10, max = 100) String jobId) {
        return detailingClaimRepository.findByJobId(jobId)
                .map(toFileSets::apply)
                .map(Map::values)
                .flatMap(Flux::fromIterable)
                .map(fileSetDtoMapper::toGetDto);
    }

    /**
     * Gets archive (zip) of files in a FileSet.
     * The method is only accessible to Internal users
     * @param currentUser
     * @param jobId
     * @param fileSetId
     * @return DataBuffer Flux
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Flux<DataBuffer> getFileSetAsZip(@NotNull String currentUser,
                                            @NotNull @Size(min = 10, max = 100) String jobId,
                                            @NotNull Date claimDate,
                                            @NotNull @Size(min = 10, max = 100) String fileSetId
    ) {
        return detailingClaimRepository.findClaim(jobId, currentUser, claimDate)
                        .switchIfEmpty(Mono.error(DetailingFileServiceException.noClaimFound(jobId, currentUser)))
                        .doOnError(error -> logMessageSender.sendSystemInfo("(completeDetailingClaim): " + error.getMessage()))
                .<FileSet>handle((detailingClaim, sink) -> {
                        var fileSet = findFileSet.apply(detailingClaim, fileSetId);
                        if (fileSet != null) sink.next(fileSet);
                        else sink.error(DetailingFileServiceException.noFileSetForId(fileSetId));
                })
                        .doOnError(error -> logMessageSender.sendSystemInfo("(completeDetailingClaim): " + error.getMessage()))
                .map(toZipArchivePath::apply)
                        .doOnNext(path -> logMessageSender.sendBusinessInfo("Download FileSet Zip Archive: Path=" + path.path() + ", Rel=" + path.isRelative()))
                .flatMapMany(this::loadFileAsDataBuffer);
    }
    // </editor-fold>

    // <editor-fold desc="Instance Helper methods">
    private DetailingFile processDetailingFile(DetailingFile detailingFile) {
        if (!toIsProcessed.apply(detailingFile)) {
            try {
                var data = fileProcessor.processFileData(detailingFile, fileStorage.getRoot());
                detailingFile = detailingFile.withFileData(data).withIsProcessed(true);
            } catch (FileProcessorException e) {
                log.error("File processing failed with the following error: {}", e.getMessage());
                detailingFile = detailingFile.withIsProcessed(false);
            }
        }
        return detailingFile;
    }

    private DetailingClaim addDetailingFilesToDetailingClaimFileSet(String fileSetId, List<DetailingFile> processedDetailingFiles, DetailingClaim detailingClaim) {
        var fileSets = toFileSets.apply(detailingClaim); // get current file sets map from detailing claim
        fileSets.computeIfPresent(fileSetId, (key, value) -> value.withFiles(processedDetailingFiles)); // only update an existing fileSet (File set should exist)
        return detailingClaim.withFileSets(fileSets);
    }

    private FileSet createFileSetFromFiles(ArchiveContents archiveContents) {
        var id = idGenerator.generateUuid();
        var xPath = XPath.relative(archiveContents.getRelativeArchivePath());
        var unprocessedDetailingFiles = DetailingFile.createUnprocessedDetailingFileList(archiveContents.getContainedFileRelativePaths());
        return FileSet.createNewFileSet(id, xPath, unprocessedDetailingFiles);
    }
    // </editor-fold>

    // <editor-fold desc="Instance Webflux Helper methods">
    /**
     * Unzip zip archive
     * @param absoluteZipPath
     * @param sink
     */
    private void unzipArchiveOrError(Path absoluteZipPath, SynchronousSink<ArchiveContents> sink) {
        try {
            sink.next(zipFileStorage.unzipArchive(absoluteZipPath, false));
        } catch (ZipFileStorageException e) {
            sink.error(e);
        }
    }

    /**
     * Loads a file to a DataBuffer Flux
     * @param path
     * @return DataBuffer Flux
     */
    private Flux<DataBuffer> loadFileAsDataBuffer(XPath path) {
        if (path.isRelative()) {
            return fileStorage.loadFromRoot(path.path());
        } else {
            var relativePath = fileStorage.makeRelative(path.toPath());
            return fileStorage.loadFromRoot(relativePath);
        }
    }

    /**
     * Populates file info to DetailingFile object.
     * Use before extracting / processing scheduling data
     * @param detailingFile
     * @return
     */
    private Mono<DetailingFile> preprocessDetailingFile(DetailingFile detailingFile) {
        var _filePath = toPath.apply(detailingFile);
        if (_filePath == null || _filePath.path() == null || _filePath.path().isEmpty()) {
            return Mono.error(DetailingFileServiceException.detailingFilePathIsNull());
        }
        Path filePath;
        if (_filePath.isRelative()) {
            filePath = fileStorage.makeAbsolute(Paths.get(_filePath.path()));
        } else {
            filePath = Paths.get(_filePath.path());
        }
        detailingFile = detailingFile.withFileSize(tryGetFileSize(filePath));
        detailingFile = detailingFile.withMimeType(tryGetMimeType(filePath));
        return Mono.just(detailingFile);
    }

    private Mono<Path> storeFileMono(Mono<FilePart> filePartMono, DetailingClaim detailingClaim) {
        return filePartMono
                .flatMap(filePart -> {
                    var clientPathPart = toClientIdentifier.apply(detailingClaim); // TODO: Remove this and replace with line below
                    var jobNumber = toJobNumber.apply(detailingClaim);
                    var jobId = toJobId.apply(detailingClaim);
                    var claimIdPathPart = jobId + "_" + new SimpleDateFormat("dd-MM-yyyy_HH.mm.ss.S").format(new Date());
                    return fileStorage.saveToRoot(filePart, clientPathPart, jobNumber.toString(), claimIdPathPart);
                });
    }

    /**
     * Saves File Set to Detailing Claim
     * @param fileSet
     * @param detailingClaim
     * @return Mono of the saved FileSet
     */
    private Mono<FileSet> saveFileSetToDetailingClaim(FileSet fileSet, DetailingClaim detailingClaim) {
        var detailingClaimWithFileSet = detailingClaim.addFileSet(fileSet);
        var fileSetId = toFileSetId.apply(fileSet);
        return detailingClaimRepository.save(detailingClaimWithFileSet)
                .map(savedDetailingClaim -> toFileSets.apply(savedDetailingClaim).get(fileSetId).withJobClaim(savedDetailingClaim));
    }

    /**
     * Access claim only if same user
     * @param jobId
     * @param currentUser
     * @return Detailing Claim Mono, otherwise Error
     */
    private Mono<DetailingClaim> getActiveDetailingClaimIfAllowed(String jobId, String currentUser) {
        return detailingClaimRepository.findActiveClaim(jobId, currentUser)
                .switchIfEmpty(Mono.error(DetailingFileServiceException.noClaimFound(jobId, currentUser)))
                .filter(detailingClaim -> DetailingClaimFacade.toClaimedByStaffUsername.apply(detailingClaim).equals(currentUser))
                .switchIfEmpty(Mono.error(DetailingFileServiceException.notClaimOwner()))
                .log();
    }
    // </editor-fold>

    // <editor-fold desc="Static helper methods">
    /**
     * Tries to get the size of a file with Path
     * @param filePath
     * @return Long file size or -1 if error
     */
    private static Long tryGetFileSize(Path filePath) {
        try {
            return Files.size(filePath);
        } catch (IOException e) {
            log.warn("Unable to access file {}", filePath);
            return -1l;
        }
    }

    /**
     * Tries to get mime type for file with Path
     * @param filePath
     * @return String mimetype or empty string if access error
     */
    private static String tryGetMimeType(Path filePath) {
        var file = filePath.toFile();
        var mimeType = FileHelper.getMimeType(file);
        if (mimeType == null) {
            log.warn("Unable to access file: {}", filePath);
            mimeType = "";
        }
        return mimeType;
    }
    // </editor-fold>

}
