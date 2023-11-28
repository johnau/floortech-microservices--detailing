package tech.jmcs.floortech.detailing.domain.repository;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jmcs.floortech.detailing.domain.configs.CrudRepository;
import tech.jmcs.floortech.detailing.domain.configs.DetailingStatus;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;

import java.util.Date;
import java.util.List;

public interface DetailingClaimRepository extends CrudRepository<DetailingClaim, String> {
    Flux<DetailingClaim> findClaims(String jobId, String username);
    Mono<DetailingClaim> findClaim(String jobId, String username, Date claimedDate);
    Mono<DetailingClaim> findActiveClaim(String jobId, String username);
    Mono<DetailingClaim> findPausedClaim(String jobId, String username);

    Mono<DetailingClaim> findByIdAndActive(String claimId);
    Flux<DetailingClaim> findAll(Pageable paging);
    Flux<DetailingClaim> findAllActive(Pageable paging);
    Flux<DetailingClaim> findAllByStaffUsernameAndActive(String username, Pageable paging);
    Flux<DetailingClaim> findByJobId(String jobId);
    Flux<DetailingClaim> findByJobIds(List<String> jobIds);
    Mono<DetailingClaim> findByJobIdAndUnverified(String jobId);
    Flux<DetailingClaim> findByJobIdAndNotStatus(String jobId, DetailingStatus notStatus);
    Mono<DetailingClaim> findByJobIdAndClaimed(String jobId);
    Mono<DetailingClaim> findByIdAndClaimed(String claimId);
    Flux<DetailingClaim> findAllByStaffUsernameAndCompleted(String username, Pageable paging);

    Flux<DetailingClaim> findAllByStaffUsernameAndCancelled(String username, Pageable paging);

}
