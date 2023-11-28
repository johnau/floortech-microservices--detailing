package tech.jmcs.floortech.detailing.app.service;


import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tech.jmcs.floortech.detailing.domain.service.FileStorageService;
import tech.jmcs.floortech.detailing.infrastructure.files.storage.FileStorageServiceImpl;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@WebFluxTest(FileStorageServiceImpl.class)
@ExtendWith(SpringExtension.class)
public class FileStorageTests { //} implements IntegrationTestWithoutEurekaClient {

    @Autowired
    private FileStorageService fileStorage;

    @Test
    @Order(1)
    public void shouldSaveToRoot() {
        FilePart filePart = Mockito.mock(FilePart.class);
        given(filePart.filename()).willReturn("TestFile.zip");
        given(filePart.transferTo(any(Path.class))).willReturn(Mono.empty());

        var resultMono = fileStorage.saveToRoot(filePart, "a", "b", "c");
        StepVerifier.create(resultMono)
                .consumeNextWith(path -> {
                    System.out.println("Test result output (path): " + path);
                    assertEquals(true, path.toString().contains("TestFile") && path.toString().endsWith(".zip"));
                    assertEquals(true, path.toString().contains("a\\b\\c\\TestFile") && path.toString().endsWith(".zip"));
                })
                .verifyComplete();
    }

    @Test
    @Order(2)
    public void shouldDeleteFromRoot() {
        var resultPath = fileStorage.softDelete("a\\b\\c\\TestFile.zip");
        assertEquals(true, resultPath.toString().contains("a\\b\\c\\TestFile"));
        assertEquals(true, resultPath.toString().endsWith(".zip"));
        assertEquals(true, resultPath.toString().startsWith(FileStorageServiceImpl.DELETED_FOLDER_PREFIX));
    }

    @Test
    public void shouldSaveToTemp() {
        FilePart filePart = Mockito.mock(FilePart.class);
        given(filePart.filename()).willReturn("TestFile.zip");
        given(filePart.transferTo(any(Path.class))).willReturn(Mono.empty());

        var resultMono = fileStorage.saveToTemp(Mono.just(filePart), "a", "b", "c");
        StepVerifier.create(resultMono)
                .consumeNextWith(path -> {
                    System.out.println("Test result output (path): " + path);
                    assertEquals(true, path.contains("TestFile") && path.endsWith(".zip"));
                    assertEquals(true, path.contains("a\\b\\c\\TestFile") && path.endsWith(".zip"));
                })
                .verifyComplete();
    }

    @Test
    public void shouldMakeRelative() {
        var absolute = Paths.get("D:\\temp\\tests\\ft_uploads\\_ft_detailing2\\a\\b\\c");
        var relative = fileStorage.makeRelative(absolute);
        System.out.println(relative);
        assertEquals(Paths.get("\\a\\b\\c"), relative);
    }
}
