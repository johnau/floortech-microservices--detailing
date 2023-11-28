package tech.jmcs.floortech.detailing.app.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.mock.web.MockMultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Path;

class MockFilePartForTests implements FilePart {
    String filename;
    MockMultipartFile mockMultipartFile;

    public MockFilePartForTests(String filename, MockMultipartFile mockMultipartFile) {
        this.filename = filename;
        this.mockMultipartFile = mockMultipartFile;
    }

    @Override
    public String filename() {
        var fromMock = mockMultipartFile.getOriginalFilename();
        if (fromMock != null && !fromMock.isEmpty()) {
            return fromMock;
        }
        return filename;
    }

    @Override
    public Mono<Void> transferTo(Path dest) {
        try {

            System.out.println("Writing stream to dest=" + dest + " size=" + mockMultipartFile.getSize());
            mockMultipartFile.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Mono.empty();
    }

    @Override
    public String name() {
        return mockMultipartFile.getName();
    }

    @Override
    public HttpHeaders headers() {
        return new HttpHeaders();
    }

    @Override
    public Flux<DataBuffer> content() {
        return DataBufferUtils.readInputStream(() -> mockMultipartFile.getInputStream(), new DefaultDataBufferFactory(), 64);
    }
}
