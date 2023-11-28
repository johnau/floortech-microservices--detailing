package tech.jmcs.floortech.detailing.presentation.web.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import tech.jmcs.floortech.detailing.app.service.DetailingFileService;
import tech.jmcs.floortech.detailing.app.dto.GetFileSetDto;
import tech.jmcs.floortech.detailing.presentation.web.config.GlobalRoutingHandler;

import java.awt.image.DataBuffer;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class DetailingFileSetHandlers {
    static final Logger log = LoggerFactory.getLogger(DetailingFileSetHandlers.class);
    final DetailingFileService detailingFileService;

    public DetailingFileSetHandlers(DetailingFileService detailingFileService) {
        this.detailingFileService = detailingFileService;
    }

    public Mono<ServerResponse> handleSubmitFileSet(ServerRequest serverRequest) {
        var claimId = serverRequest.pathVariable("claimId");
        var multipartFile = serverRequest.multipartData();
        var filePartMono = multipartFile.map(map -> (FilePart) map.toSingleValueMap().get("file"));
        var contentLength = serverRequest.headers().contentLength().orElse(-1L);

        return serverRequest.principal()
                .map(Principal::getName)
                        .doOnNext(username -> log.info("User {} is submitting file set for detailing claim {}", username, claimId))
                .flatMap(username -> GlobalRoutingHandler.doRequest(detailingFileService.submitDetailingFilesArchive(username, claimId, filePartMono, contentLength), HttpStatus.OK));
    }

    public Mono<ServerResponse> handleProcessFileSet(ServerRequest serverRequest) {
        var claimId = serverRequest.pathVariable("claimId");
        var fileSetId = serverRequest.pathVariable("fileSetId");
        return serverRequest.principal()
                .map(Principal::getName)
                        .doOnNext(username -> log.info("User {} is processing file set {} for detailing claim {}", username, fileSetId, claimId))
                .flatMap(username -> GlobalRoutingHandler.doRequest(detailingFileService.processFileSet(username, claimId, fileSetId), HttpStatus.OK));
    }

    public Mono<ServerResponse> handleRetrieveFileSetAsZip(ServerRequest serverRequest) {
//        var claimId = serverRequest.pathVariable("claimId");
        var jobId = serverRequest.pathVariable("jobId");
        var fileSetId = serverRequest.pathVariable("fileSetId");
        var dateStr = serverRequest.pathVariable("claimedDate");

        var sdf = new SimpleDateFormat("dd-MM-yyyy_hh:mm:ssZ"); // TODO: Move date format to global date formatting with Spring
        try {
            var date = sdf.parse(dateStr);
            return serverRequest.principal()
                    .map(Principal::getName)
                    .doOnNext(username -> log.info("User {} retrieving detailing claim file set {} for {}", username, fileSetId, jobId))
                    .flatMap(username -> ServerResponse
                            .status(HttpStatus.OK)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(detailingFileService.getFileSetAsZip(username, jobId, date, fileSetId), DataBuffer.class)
                    );
        } catch (ParseException e) {
            return Mono.error(new Exception("Bad date format"));
        }
    }

    public Mono<ServerResponse> handleRetrieveAllJobFileSets(ServerRequest serverRequest) {
        var jobId =  serverRequest.pathVariable("jobID");
        return serverRequest.principal()
                .map(Principal::getName)
                        .doOnNext(username -> log.info("User {} retrieving all file set for job {}", username, jobId))
                .flatMap(username -> ServerResponse
                        .status(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(detailingFileService.getAllFileSetsForJobId(jobId), GetFileSetDto.class)
                );
    }
}
