package tech.jmcs.floortech.detailing.infrastructure.files.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tech.jmcs.floortech.common.helper.ArchiveContents;
import tech.jmcs.floortech.common.helper.FileHelper;
import tech.jmcs.floortech.detailing.domain.service.exception.ZipFileStorageException;
import tech.jmcs.floortech.detailing.domain.service.FileStorageService;
import tech.jmcs.floortech.detailing.domain.service.ZipFileStorageService;

import java.io.*;
import java.nio.file.Path;

@Service
public class ZipFileStorageServiceImpl implements ZipFileStorageService {
    static final Logger log = LoggerFactory.getLogger(ZipFileStorageServiceImpl.class);
    final FileStorageService fileStorage;

    @Autowired
    public ZipFileStorageServiceImpl(FileStorageService fileStorage) {
        this.fileStorage = fileStorage;
    }

    /**
     * Unzip archive
     * @param zipPath
     * @param isRelative
     * @return ArchiveContents object with zip archive path and extracted file contents paths
     * @throws ZipFileStorageException
     */
    @Override
    public ArchiveContents unzipArchive(Path zipPath, boolean isRelative) throws ZipFileStorageException {
        Path absoluteZipPath = determineAbsolute(zipPath, isRelative);
        Path relativeZipPath = determineRelative(zipPath, isRelative);

        var archiveContents = new ArchiveContents(relativeZipPath, absoluteZipPath);
        try {
            return FileHelper.unzipArchive(fileStorage.getRoot(), archiveContents, relativeZipPath.toString());
        } catch (IOException e) {
            throw new ZipFileStorageException(e.getMessage());
        }
    }

    @Override
    public Mono<ArchiveContents> unzipArchiveMono(Path zipPath, boolean isRelative) {
        try {
            return Mono.just(unzipArchive(zipPath, isRelative));
        } catch (ZipFileStorageException e) {
            return Mono.error(e);
        }
    }

    /**
     * Saves a Zip File to storage root and extracts contents to temporary storage, returning relative and absolute
     * paths to all contained files and the zip archive.
     * @param filePart
     * @param pathParts
     * @return
     */
    @Override
    public Mono<ArchiveContents> saveArchiveAndExtract(FilePart filePart, String... pathParts) {
        return fileStorage
                .saveToRoot(filePart, pathParts)
                .map(absoluteZipPath -> new ArchiveContents(absoluteZipPath, fileStorage.makeRelative(absoluteZipPath)))
                .<ArchiveContents>handle((archiveContents, sink) -> {
                    try {
                        sink.next(FileHelper.unzipArchive(fileStorage.getRoot(), archiveContents, pathParts));
                    } catch (IOException e) {
                        sink.error(e);
                    }
                })
                .doOnNext(archiveContents -> log.info("Saved archive to: " + archiveContents.getAbsoluteArchivePath()))
                .doOnNext(archiveContents -> log.info("Archive Contents extracted. File list=" + archiveContents.getContainedFileAbsolutePaths()));
    }

    private Path determineRelative(Path zipPath, boolean isRelative) {
        if (isRelative) {
            return zipPath;
        } else {
            return fileStorage.makeRelative(zipPath);
        }
    }

    private Path determineAbsolute(Path zipPath, boolean isRelative) {
        if (isRelative) {
            return fileStorage.makeAbsolute(zipPath);
        } else {
            return zipPath;
        }
    }

}
