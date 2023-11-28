package tech.jmcs.floortech.detailing.domain.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jmcs.floortech.detailing.domain.configs.XPath;

import java.nio.file.Path;

public interface FileStorageService {
    Path getRoot();
    Path makeRelative(Path path);
    Path makeAbsolute(Path relativePath);
    Path softDelete(String relativePath);
    Mono<Path> saveToRoot(FilePart filePart, String... pathParts);
    Mono<String> saveToTemp(Mono<FilePart> filePartMono, String... pathParts);
    Flux<DataBuffer> loadFromRoot(String relativePath);
    Flux<DataBuffer> loadFromRoot(Path relativePath);
    Flux<DataBuffer> loadFromRoot(XPath xPath);
}
