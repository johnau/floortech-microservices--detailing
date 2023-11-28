package tech.jmcs.floortech.detailing.presentation.web.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import tech.jmcs.floortech.detailing.app.service.DetailingClaimService;
import tech.jmcs.floortech.detailing.app.dto.GetDetailingClaimDto;
import tech.jmcs.floortech.detailing.presentation.web.config.GlobalRoutingHandler;

import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@Component
public class DetailingClaimHandlers {
    static final Logger log = LoggerFactory.getLogger(DetailingClaimHandlers.class);
    final DetailingClaimService detailingClaimService;
    @Autowired
    public DetailingClaimHandlers(DetailingClaimService detailingClaimService) {
        this.detailingClaimService = detailingClaimService;
    }

    public Mono<ServerResponse> handleNewClaim(ServerRequest serverRequest) {
        var jobId = serverRequest.pathVariable("jobId");
        return serverRequest.principal()
                .map(Principal::getName)
                        .doOnNext(username -> log.info("User {} is claiming job {}", username, jobId))
                .flatMap(username -> GlobalRoutingHandler.doRequest(detailingClaimService.claimDetailingJob(username, jobId), HttpStatus.OK));
    }

    public Mono<ServerResponse> handleLookupClaimWithId(ServerRequest serverRequest) {
        var claimId = serverRequest.pathVariable("claimId");
        log.info("Looking up claim: {}", claimId);
        return GlobalRoutingHandler.doRequest(detailingClaimService.getDetailingClaim(claimId), HttpStatus.OK);
    }

    public Mono<ServerResponse> handleLookupClaim(ServerRequest serverRequest) {
        var jobId = serverRequest.pathVariable("jobId");
        var claimedByUsername = serverRequest.pathVariable("user");
        var dateStr = serverRequest.pathVariable("claimedDate");

        var sdf = new SimpleDateFormat("dd-MM-yyyy_hh:mm:ssZ"); // TODO: Move date format to global date formatting with Spring

        try {
            var date = sdf.parse(dateStr);
            log.info("Looking up claim: {} + {} + {}", jobId, claimedByUsername, date);
            return GlobalRoutingHandler.doRequest(detailingClaimService.getDetailingClaim(jobId, claimedByUsername, date), HttpStatus.OK);
        } catch (ParseException e) {
            return Mono.error(new Exception("Bad date format"));
        }

    }

    public Mono<ServerResponse> handleLookupAllActiveClaims(ServerRequest serverRequest) {
        var paging = createPagingRequest(serverRequest.queryParam("page"), serverRequest.queryParam("size"))
                .orElse(PageRequest.of(0, 50));
        return ServerResponse
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(detailingClaimService.getAllActiveClaims(paging), GetDetailingClaimDto.class);
    }

    public Mono<ServerResponse> handleReleaseDetailingClaim(ServerRequest serverRequest) {
        var claimId = serverRequest.pathVariable("claimId");
        return serverRequest.principal()
                .map(Principal::getName)
                        .doOnNext(username -> log.info("User {} is cancelling claim {}", username, claimId))
                .flatMap(username -> GlobalRoutingHandler.doRequest(detailingClaimService.releaseDetailingClaim(username, claimId), HttpStatus.OK));
    }

    public Mono<ServerResponse> handlePauseDetailingClaim(ServerRequest serverRequest) {
        var claimId = serverRequest.pathVariable("claimId");
        return serverRequest.principal()
                .map(Principal::getName)
                        .doOnNext(username -> log.info("User {} is pausing claim {}", username, claimId))
                .flatMap(username -> GlobalRoutingHandler.doRequest(detailingClaimService.pauseDetailingClaim(username, claimId), HttpStatus.OK));
    }

    public Mono<ServerResponse> handleResumeDetailingClaim(ServerRequest serverRequest) {
        var claimId = serverRequest.pathVariable("claimId");
        return serverRequest.principal()
                .map(Principal::getName)
                        .doOnNext(username -> log.info("User {} is resuming claim {}", username, claimId))
                .flatMap(username -> GlobalRoutingHandler.doRequest(detailingClaimService.resumeDetailingClaim(username, claimId), HttpStatus.OK));
    }

    public Mono<ServerResponse> handleCompleteDetailingClaim(ServerRequest serverRequest) {
        var claimId = serverRequest.pathVariable("claimId");
        return serverRequest.principal()
                .map(Principal::getName)
                        .doOnNext(username -> log.info("User {} is completing claim {}", username, claimId))
                .flatMap(username -> GlobalRoutingHandler.doRequest(detailingClaimService.completeDetailingClaim(username, claimId), HttpStatus.OK));
    }

    public Mono<ServerResponse> handleLookupClaimsForUser(ServerRequest serverRequest) {
        var paging = createPagingRequest(serverRequest.queryParam("page"), serverRequest.queryParam("size"))
                .orElse(PageRequest.of(0, 50));
        var username = serverRequest.pathVariable("username");
        return ServerResponse
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(detailingClaimService.getAllClaimsByStaffUsernameAndActive(username, paging), GetDetailingClaimDto.class);
    }

    public Mono<ServerResponse> handleLookupMyClaims(ServerRequest serverRequest) {
        var paging = createPagingRequest(serverRequest.queryParam("page"), serverRequest.queryParam("size"))
                .orElse(PageRequest.of(0, 50));
        return serverRequest.principal()
                .map(Principal::getName)
                        .doOnNext(username -> log.info("User {} is looking up their claims", username))
                .flatMap(username -> ServerResponse
                        .status(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(detailingClaimService.getAllClaimsByStaffUsernameAndActive(username, paging), GetDetailingClaimDto.class)
                );
    }

    public Mono<ServerResponse> handleLookupClaimByJobId(ServerRequest serverRequest) {
        var jobId = serverRequest.pathVariable("jobId");
        return GlobalRoutingHandler.doRequest(detailingClaimService.getCurrentActiveClaimByJobId(jobId), HttpStatus.OK);
    }

    private Optional<PageRequest> createPagingRequest(Optional<String> page, Optional<String> size) {
        if (page.isEmpty() || size.isEmpty()) return Optional.empty();
        var pageNum = 0;
        var pageSize = 0;
        try {
            pageNum = Integer.parseInt(page.get());
        } catch (NumberFormatException ex) {}
        try {
            pageSize = Integer.parseInt(size.get());
        } catch (NumberFormatException ex) {}
        return Optional.of(PageRequest.of(pageNum, pageSize));
    }

}
