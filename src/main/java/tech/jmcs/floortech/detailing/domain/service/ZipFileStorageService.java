package tech.jmcs.floortech.detailing.domain.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import tech.jmcs.floortech.common.helper.ArchiveContents;
import tech.jmcs.floortech.detailing.domain.service.exception.ZipFileStorageException;

import java.nio.file.Path;

public interface ZipFileStorageService {
    Mono<ArchiveContents> saveArchiveAndExtract(FilePart filePart, String... pathParts);
    Mono<ArchiveContents> unzipArchiveMono(Path zipPath, boolean isRelative);
    ArchiveContents unzipArchive(Path zipPath, boolean isRelative) throws ZipFileStorageException;
}
