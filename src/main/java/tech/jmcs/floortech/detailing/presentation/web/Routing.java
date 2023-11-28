package tech.jmcs.floortech.detailing.presentation.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;
import tech.jmcs.floortech.detailing.presentation.web.handlers.DetailingClaimHandlers;
import tech.jmcs.floortech.detailing.presentation.web.handlers.DetailingFileSetHandlers;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class Routing {

    @Bean
    public RouterFunction<ServerResponse> routingFunction(DetailingClaimHandlers detailingClaimHandlers, DetailingFileSetHandlers detailingFileSetHandlers) {
        return RouterFunctions
                .nest(
                        path("/detailing"),
                        RouterFunctions
                                .route(POST("/claim/job/{jobId}").and(accept(MediaType.APPLICATION_JSON)), detailingClaimHandlers::handleNewClaim)
                                .andRoute(GET("/claim/{jobId}/{user}/{date}").and(accept(MediaType.APPLICATION_JSON)), detailingClaimHandlers::handleLookupClaim)
                                .andRoute(GET("/claim/{claimId}").and(accept(MediaType.APPLICATION_JSON)), detailingClaimHandlers::handleLookupClaimWithId)
                                .andRoute(GET("/all/active").and(accept(MediaType.APPLICATION_JSON)), detailingClaimHandlers::handleLookupAllActiveClaims)
                                .andRoute(POST("/pause/{claimId}").and(accept(MediaType.APPLICATION_JSON)), detailingClaimHandlers::handlePauseDetailingClaim)
                                .andRoute(POST("/resume/{claimId}").and(accept(MediaType.APPLICATION_JSON)), detailingClaimHandlers::handleResumeDetailingClaim)
                                .andRoute(POST("/cancel/{claimId}").and(accept(MediaType.APPLICATION_JSON)), detailingClaimHandlers::handleReleaseDetailingClaim)
                                .andRoute(POST("/complete/{claimId}").and(accept(MediaType.APPLICATION_JSON)), detailingClaimHandlers::handleCompleteDetailingClaim)
                                .andRoute(GET("/claimed/by/{username}").and(accept(MediaType.APPLICATION_JSON)), detailingClaimHandlers::handleLookupClaimsForUser)
                                .andRoute(GET("/my-claims").and(accept(MediaType.APPLICATION_JSON)), detailingClaimHandlers::handleLookupMyClaims)
                                .andRoute(GET("/claim/for/job/{jobId}").and(accept(MediaType.APPLICATION_JSON)), detailingClaimHandlers::handleLookupClaimByJobId)
                )
                .andNest(
                        path("/detailing-files"),
                        RouterFunctions
                                .route(POST("/submit/for/{claimId}").and(accept(MediaType.APPLICATION_JSON)), detailingFileSetHandlers::handleSubmitFileSet)
                                .andRoute(GET("/process-file-set/{fileSetId}/of/{claimId}").and(accept(MediaType.APPLICATION_JSON)), detailingFileSetHandlers::handleProcessFileSet)
                                .andRoute(GET("/get-zip/{fileSetId}/of/{claimId}").and(accept(MediaType.APPLICATION_JSON)), detailingFileSetHandlers::handleRetrieveFileSetAsZip)
                                .andRoute(GET("/get-all-sets/for-job/{jobId}").and(accept(MediaType.APPLICATION_JSON)), detailingFileSetHandlers::handleRetrieveAllJobFileSets)
                );
    }
}