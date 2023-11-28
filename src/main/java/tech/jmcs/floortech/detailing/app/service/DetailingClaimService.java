package tech.jmcs.floortech.detailing.app.service;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import tech.jmcs.floortech.common.auth.IsAuthenticatedAsFloortechInternalUser;
import tech.jmcs.floortech.common.dto.AppUserDto;
import tech.jmcs.floortech.common.dto.FloortechJobDto;
import tech.jmcs.floortech.detailing.app.dto.mapper.DetailingClaimDtoMapper;
import tech.jmcs.floortech.detailing.app.service.exception.DetailingClaimServiceException;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.app.dto.GetDetailingClaimDto;
import tech.jmcs.floortech.detailing.domain.repository.DetailingClaimRepository;
import tech.jmcs.floortech.detailing.domain.service.FloortechJobDataService;
import tech.jmcs.floortech.detailing.domain.service.RemoteLoggingService;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim.newUnverifiedClaim;
import static tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaimFacade.*;

@Service
@Validated
public class DetailingClaimService {
    static final Logger log = LoggerFactory.getLogger(DetailingFileService.class);
    final DetailingClaimRepository detailingClaimRepository;
    final Converter<byte[], FloortechJobDto> floortechJobDtoConverter;
    final Converter<byte[], AppUserDto> appUserDtoConverter;
    final RemoteLoggingService logMessageSender;
    final DetailingClaimDtoMapper detailingClaimDtoMapper;
    final FloortechJobDataService floortechJobDataService;

    @Autowired
    public DetailingClaimService(DetailingClaimRepository detailingClaimRepository, Converter<byte[], FloortechJobDto> floortechJobDtoConverter, Converter<byte[], AppUserDto> appUserDtoConverter, RemoteLoggingService logMessageSender, DetailingClaimDtoMapper detailingClaimDtoMapper, FloortechJobDataService floortechJobDataService) {
        this.detailingClaimRepository = detailingClaimRepository;
        this.floortechJobDtoConverter = floortechJobDtoConverter;
        this.appUserDtoConverter = appUserDtoConverter;
        this.logMessageSender = logMessageSender;
        this.detailingClaimDtoMapper = detailingClaimDtoMapper;
        this.floortechJobDataService = floortechJobDataService;
    }

    // <editor-fold desc="Webflux methods">

    /**
     * Webflux method to claim a job for detailing.
     * The only method that creates a claim
     * This method will call the Job Service for extra job information.
     * User cannot claim a job if already claimed (claim exists with Unverified, Started, or Paused status)
     * ConstraintViolationException thrown if currentUser is null or jobId is null or invalid
     * @param currentUser
     * @param jobId
     * @return
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Mono<GetDetailingClaimDto> claimDetailingJob(@NotNull String currentUser, @NotNull @Size(min = 10, max = 100) String jobId) {
        return detailingClaimRepository.findByJobIdAndClaimed(jobId) // find any existing claims
                .onErrorStop() // do not continue processing on error
                .<DetailingClaim>handle((detailingClaim, sink) -> { // if existing, check if currentUser is owner
                    if (isOwnedBy.apply(detailingClaim, currentUser)) sink.next(detailingClaim);
                    else sink.error(DetailingClaimServiceException.jobAlreadyClaimed("Other user"));
                })
                .handle(DetailingClaimService::switchExisting) // if already claimed, exit, else try to verify unverified
                .switchIfEmpty(detailingClaimRepository.save(newUnverifiedClaim(jobId, currentUser))).doOnNext(detailingClaim -> log.info("Processing unverified claim for: {} - {}", toJobId.apply(detailingClaim), toClaimedByStaffUsername.apply(detailingClaim)))
                .flatMap(floortechJobDataService::requestDataAndUpdate) // will throw an error if it can't contact - ending the stream
                .handle(DetailingClaimService::verifyAndStartOrError)
                .flatMap(detailingClaimRepository::save).log()
                .map(detailingClaimDtoMapper::toGetDto).doOnNext(detailingClaim -> logMessageSender.sendBusinessInfo("Detailing Job Claimed: ID=" + detailingClaim.jobId() + ", User=" + currentUser));
    }

    /**
     * Fetches a detailing claim by id
     * @param claimId
     * @return
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Mono<GetDetailingClaimDto> getDetailingClaim(@NotNull @Size(min = 10, max = 100) String claimId) {
        return detailingClaimRepository.findById(claimId)
                        .switchIfEmpty(Mono.error(DetailingClaimServiceException.claimIdDoesNotExist(claimId)))
                .map(detailingClaimDtoMapper::toGetDto).log();
    }

    @IsAuthenticatedAsFloortechInternalUser
    public Mono<String> getDetailingClaimId(@NotNull @Size(min = 10, max = 100) String jobId,
                                            @NotNull String claimedByUsername,
                                            @NotNull Date claimedDate) {
        return Mono.empty(); // TODO: Implement lookup of ID from db
    }

    /**
     * Fetches a detailing claim by id
     * @param jobId
     * @return
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Mono<GetDetailingClaimDto> getDetailingClaim(@NotNull @Size(min = 10, max = 100) String jobId,
                                                        @NotNull String claimedByUsername,
                                                        @NotNull Date claimedAt) {
        return detailingClaimRepository.findClaim(jobId, claimedByUsername, claimedAt)
                .switchIfEmpty(Mono.error(DetailingClaimServiceException.claimNotFound()))
                .map(detailingClaimDtoMapper::toGetDto).log();
    }

    @IsAuthenticatedAsFloortechInternalUser
    public Flux<GetDetailingClaimDto> getDetailingClaims(@NotNull @Size(min = 10, max = 100) String jobId,
                                                        @NotNull String claimedByUsername) {
        return detailingClaimRepository.findClaims(jobId, claimedByUsername)
                .switchIfEmpty(Mono.error(DetailingClaimServiceException.claimNotFound()))
                .map(detailingClaimDtoMapper::toGetDto).log();
    }

    /**
     * Get all active claims (Started, Paused status)
     * @param paging
     * @return
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Flux<GetDetailingClaimDto> getAllActiveClaims(Pageable paging) {
        if (paging == null) paging = PageRequest.of(0, 50);
        return detailingClaimRepository.findAllActive(paging)
                .map(detailingClaimDtoMapper::toGetDto)
                        .doOnNext(detailingClaim -> logMessageSender.sendSystemInfo("Claim retrieved: ID=" + detailingClaim.id()));
    }

    /**
     * Cancel a claim, must be claim owner
     * @param currentUser
     * @param jobId
     * @return
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Mono<GetDetailingClaimDto> releaseDetailingClaim(@NotNull String currentUser, @NotNull @Size(min = 10, max = 100) String jobId) {
        return detailingClaimRepository.findActiveClaim(jobId, currentUser)
                .switchIfEmpty(detailingClaimRepository.findPausedClaim(jobId, currentUser))
                        .switchIfEmpty(Mono.error(DetailingClaimServiceException.claimNotFound(jobId, currentUser)))
                .onErrorStop()
                .filter(detailingClaim -> isOwnedBy.apply(detailingClaim, currentUser))
                        .switchIfEmpty(Mono.error(DetailingClaimServiceException.notClaimOwner()))
                .<DetailingClaim>handle(DetailingClaimService::verifyAndCancelOrError) // update the job status to cancelled
                .log()
                .flatMap(detailingClaimRepository::save)
                        .doOnError(error -> log.info("Unable to save: " + error.getMessage()))
                .map(detailingClaimDtoMapper::toGetDto);
    }

    /**
     * Pause a claim - must be claim owner
     * @param currentUser
     * @param jobId
     * @return
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Mono<GetDetailingClaimDto> pauseDetailingClaim(@NotNull String currentUser, @NotNull @Size(min = 10, max = 100) String jobId) {
        return detailingClaimRepository.findActiveClaim(jobId, currentUser)
                .switchIfEmpty(Mono.error(DetailingClaimServiceException.claimNotFound(jobId, currentUser)))
                        .doOnError(error -> logMessageSender.sendSystemInfo("(completeDetailingClaim): " + error.getMessage()))
                .filter(detailingClaim -> isOwnedBy.apply(detailingClaim, currentUser))
                        .switchIfEmpty(Mono.error(DetailingClaimServiceException.notClaimOwner()))
                        .doOnError(error -> logMessageSender.sendSystemInfo("(completeDetailingClaim): " + error.getMessage()))
                .single()// there should only be one if database is not in an invalid state
                .<DetailingClaim>handle(DetailingClaimService::verifyAndPauseOrError)
                .flatMap(detailingClaimRepository::save)
                        .doOnError(error -> logMessageSender.sendSystemError("(completeDetailingClaim): " + error.getMessage()))
                .map(detailingClaimDtoMapper::toGetDto)
                        .doOnNext(detailingClaim -> logMessageSender.sendBusinessInfo("Detailing Job Paused: ID=" + detailingClaim.jobId() + ", Job Number=" + detailingClaim.floortechJobNumber() + ", User=" + currentUser));
    }

    /**
     * Unpause a claim, must be claim owner
     * @param currentUser
     * @param jobId
     * @return
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Mono<GetDetailingClaimDto> resumeDetailingClaim(@NotNull String currentUser, @NotNull @Size(min = 10, max = 100) String jobId) {
        return detailingClaimRepository.findPausedClaim(jobId, currentUser)
                        .switchIfEmpty(Mono.error(DetailingClaimServiceException.claimNotFound(jobId, currentUser)))
                        .doOnError(error -> logMessageSender.sendSystemInfo("(completeDetailingClaim): " + error.getMessage()))
                .filter(detailingClaim -> isOwnedBy.apply(detailingClaim, currentUser))
                        .switchIfEmpty(Mono.error(DetailingClaimServiceException.notClaimOwner()))
                        .doOnError(error -> logMessageSender.sendSystemInfo("(completeDetailingClaim): " + error.getMessage()))
                .single()
                .handle(DetailingClaimService::verifyAndStartOrError)
                .flatMap(detailingClaimRepository::save)
                        .doOnError(error -> logMessageSender.sendSystemError("(completeDetailingClaim): " + error.getMessage()))
                .map(detailingClaimDtoMapper::toGetDto)
                        .doOnNext(detailingClaim -> logMessageSender.sendBusinessInfo("Detailing Job Resumed: ID=" + detailingClaim.jobId() + ", Job Number=" + detailingClaim.floortechJobNumber() + ", User=" + currentUser));
    }

    /**
     * Complete a claim, must be claim owner, must provide a file set
     * @param currentUser
     * @param jobId
     * @return
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Mono<GetDetailingClaimDto> completeDetailingClaim(@NotNull String currentUser, @NotNull @Size(min = 10, max = 100) String jobId) {
        return detailingClaimRepository.findActiveClaim(jobId, currentUser)
                        .switchIfEmpty(Mono.error(DetailingClaimServiceException.claimNotFound(jobId, currentUser)))
                        .doOnError(error -> logMessageSender.sendSystemInfo("(completeDetailingClaim): " + error.getMessage()))
                .filter(detailingClaim -> isOwnedBy.apply(detailingClaim, currentUser))
                        .switchIfEmpty(Mono.error(DetailingClaimServiceException.notClaimOwner()))
                        .doOnError(error -> logMessageSender.sendSystemInfo("(completeDetailingClaim): " + error.getMessage()))
                .filter(hasAtLeastOneFileSet::apply)
                        .switchIfEmpty(Mono.error(DetailingClaimServiceException.cantCompleteNoFileSet()))
                        .doOnError(error -> logMessageSender.sendBusinessInfo(error.getMessage()))
                .single()
                        .doOnError(error -> logMessageSender.sendSystemError("Database error: " + error.getMessage()))
                .<DetailingClaim>handle(DetailingClaimService::verifyAndCompleteOrError)
                .flatMap(detailingClaimRepository::save)
                        .doOnError(error -> logMessageSender.sendSystemError("(completeDetailingClaim): " + error.getMessage()))
                .map(detailingClaimDtoMapper::toGetDto)
                        .doOnNext(detailingClaim -> logMessageSender.sendBusinessInfo("Detailing Job Completed: ID=" + detailingClaim.jobId() + ", Job Number=" + detailingClaim.floortechJobNumber() + ", User=" + currentUser));
    }

    /**
     * Get all claims active by user (Status is Unverified, Started, or Paused)
     * @param username
     * @param paging
     * @return Dto Flux for Active claims for user
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Flux<GetDetailingClaimDto> getAllClaimsByStaffUsernameAndActive(@NotNull String username, Pageable paging) {
        return detailingClaimRepository.findAllByStaffUsernameAndActive(username, paging)
                .map(detailingClaimDtoMapper::toGetDto);
    }

    /**
     * Get all completed claims by user
     * @param username
     * @param paging
     * @return Dto Flux of Completed claims for user
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Flux<GetDetailingClaimDto> getAllClaimsByStaffUsernameAndCompleted(@NotNull String username, Pageable paging) {
        Objects.requireNonNull(username);
        return detailingClaimRepository.findAllByStaffUsernameAndCompleted(username, paging)
                .map(detailingClaimDtoMapper::toGetDto);
    }

    /**
     * Get all cancelled claims by user
     * @param username
     * @param paging
     * @return Dto Flux of Cancelled claims for user
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Flux<GetDetailingClaimDto> getAllClaimsByStaffUsernameAndCancelled(@NotNull String username, Pageable paging) {
        Objects.requireNonNull(username);
        return detailingClaimRepository.findAllByStaffUsernameAndCancelled(username, paging)
                .map(detailingClaimDtoMapper::toGetDto);
    }

    /**
     * Get the active claim for a job by job ID
     * @param jobId
     * @return Dto Mono of claim
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Mono<GetDetailingClaimDto> getCurrentActiveClaimByJobId(@NotNull @Size(min = 10, max = 100) String jobId) {
        Objects.requireNonNull(jobId);
        return detailingClaimRepository.findByJobIdAndClaimed(jobId)
                .switchIfEmpty(Mono.error(DetailingClaimServiceException.claimForJobIdDoesNotExist(jobId)))
                .map(detailingClaimDtoMapper::toGetDto);
    }

    /**
     * Get all claims for list of jobs by job ID
     * @param jobIds
     * @return Dto Flux of all claims found by job ID
     */
    @IsAuthenticatedAsFloortechInternalUser
    public Flux<GetDetailingClaimDto> getAllClaimsForJobIds(@NotNull @Size(min = 1, max = 100) List<String> jobIds) {
        return detailingClaimRepository.findByJobIds(jobIds)
                .map(detailingClaimDtoMapper::toGetDto);
    }
    // </editor-fold>

    // <editor-fold desc="Static Webflux Helper Methods">
    /**
     * Handler to check an existing claim to see if it needs to be processed.
     * @param existingClaim
     * @param sink
     */
    private static void switchExisting(DetailingClaim existingClaim, SynchronousSink<DetailingClaim> sink) {
        switch (toStatus.apply(existingClaim)) {
            case UNVERIFIED -> sink.next(existingClaim); // retry verify an unverified claim
            case STARTED, PAUSED -> sink.error(new DetailingClaimServiceException("Already claimed")); // return existing claim for nothing to happen
        }
    }

    /**
     * Handler to verify detailing claim and set status to STARTED
     * @param detailingClaim
     * @param sink
     */
    private static void verifyAndStartOrError(DetailingClaim detailingClaim, SynchronousSink<DetailingClaim> sink) {
        try {
            sink.next(detailingClaim.verifyAndStart());
        } catch (Exception e) {
            sink.error(new DetailingClaimServiceException("Unable to verify this job claim: " + e.getMessage()));
        }
    }

    /**
     * Handler to verify detailing claim and set status to CANCELLED
     * @param claim
     * @param sink
     */
    private static void verifyAndCancelOrError(DetailingClaim claim, SynchronousSink<DetailingClaim> sink) {
        try {
            sink.next(claim.verifyAndCancel());
        } catch (Exception e) {
            sink.error(e);
        }
    }

    /**
     * Handler to verify detailing claim and set status to COMPLETED
     * @param claim
     * @param sink
     */
    private static void verifyAndCompleteOrError(DetailingClaim claim, SynchronousSink<DetailingClaim> sink) {
        try {
            sink.next(claim.verifyAndComplete());
        } catch (Exception e) {
            sink.error(new DetailingClaimServiceException(e.getMessage()));
        }
    }

    /**
     * Handler to verify detailing claim and set status to PAUSED
     * @param claim
     * @param sink
     */
    private static void verifyAndPauseOrError(DetailingClaim claim, SynchronousSink<DetailingClaim> sink) {
        try {
            sink.next(claim.verifyAndPause());
        } catch (Exception e) {
            sink.error(new DetailingClaimServiceException(e.getMessage()));
        }
    }
    // </editor-fold>
}
