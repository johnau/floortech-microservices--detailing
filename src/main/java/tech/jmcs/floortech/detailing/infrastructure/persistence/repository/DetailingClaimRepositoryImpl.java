package tech.jmcs.floortech.detailing.infrastructure.persistence.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jmcs.floortech.detailing.domain.configs.DetailingStatus;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.domain.repository.DetailingClaimRepository;
import tech.jmcs.floortech.detailing.infrastructure.persistence.dao.DetailingClaimDao;
import tech.jmcs.floortech.detailing.infrastructure.persistence.entity.DetailingClaimEntity;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static tech.jmcs.floortech.detailing.domain.configs.DetailingStatus.*;
import static tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaimFacade.*;

@Repository
public class DetailingClaimRepositoryImpl implements DetailingClaimRepository {
    final static Logger logger = LoggerFactory.getLogger(DetailingClaimRepositoryImpl.class);
    private final DetailingClaimDao detailingClaimDao;
//    private final FileSetDao fileSetDao;
    @Autowired
    public DetailingClaimRepositoryImpl(DetailingClaimDao detailingClaimDao) {
        this.detailingClaimDao = detailingClaimDao;
//        this.fileSetDao = fileSetDao;
    }

    @Override
    public Flux<DetailingClaim> findAll() {
        return detailingClaimDao.findAll()
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Flux<DetailingClaim> findByIds(List<String> ids) {
        return detailingClaimDao.findAllById(ids)
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Mono<DetailingClaim> findById(String id) {
        return detailingClaimDao.findById(id)
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Mono<DetailingClaim> save(DetailingClaim detailingClaim) {
        Objects.requireNonNull(detailingClaim);
//        var id = toClaimId.apply(detailingClaim);
        var jobId = toJobId.apply(detailingClaim);
        var claimedByStaffUsername = toClaimedByStaffUsername.apply(detailingClaim);
        var createdDate = toClaimedAt.apply(detailingClaim);
        if (createdDate == null) { // is a new claim
            logger.info("Saving a new DetailingClaim entity to database: {}, {}", jobId, claimedByStaffUsername);
            return detailingClaimDao.save(DetailingClaimEntity.fromDomainObject(detailingClaim))
                    .map(DetailingClaimEntity::toDomainObject);
        }

        logger.info("Updating a DetailingClaim entity in database: {}, {}", jobId, claimedByStaffUsername);
//        if (id != null && !id.isEmpty()) {
        return detailingClaimDao.findByCompoundId(jobId, claimedByStaffUsername, createdDate)
                .doOnNext(dce -> logger.info("Found a job with job id: {} and username: {} and created date: {}", jobId, claimedByStaffUsername, createdDate))
//                    .switchIfEmpty(detailingClaimDao.findById(id))
                .flatMap(existing -> detailingClaimDao.save(existing.updateFrom(detailingClaim)))
//                .switchIfEmpty(detailingClaimDao.save(DetailingClaimEntity.fromDomainObject(detailingClaim)))
                .log()
                .map(DetailingClaimEntity::toDomainObject);
//        }
//        return detailingClaimDao.save(DetailingClaimEntity.fromDomainObject(detailingClaim))
//                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Mono<DetailingClaim> delete(String s) {
        return null;
    }


    @Override
    public Flux<DetailingClaim> findClaims(String jobId, String username) {
        return detailingClaimDao.findByCompoundId(jobId, username)
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Mono<DetailingClaim> findClaim(String jobId, String username, Date claimedDate) {
        return detailingClaimDao.findByCompoundId(jobId, username, claimedDate)
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Mono<DetailingClaim> findActiveClaim(String jobId, String username) {
        return detailingClaimDao.findByCompoundIdAndStatus(jobId, username, STARTED)
                .singleOrEmpty()
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Mono<DetailingClaim> findPausedClaim(String jobId, String username) {
        return detailingClaimDao.findByCompoundIdAndStatus(jobId, username, PAUSED)
                .singleOrEmpty()
                .map(DetailingClaimEntity::toDomainObject);
    }

//    @Override
//    public Mono<DetailingClaim> findClaimByUserAndStatus(String jobId, String username, DetailingStatus status) {
//        return detailingClaimDao.findByCompoundIdAndStatus(jobId, username, status)
//                .map(DetailingClaimEntity::toDomainObject);
//    }

    @Override
    public Mono<DetailingClaim> findByIdAndActive(String claimId) {
        return detailingClaimDao.findByIdAndStatus(claimId, STARTED)
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Flux<DetailingClaim> findAll(Pageable paging) {
        return null;
    }

    @Override
    public Flux<DetailingClaim> findAllActive(Pageable paging) {
        return detailingClaimDao.findByStatus(STARTED, paging)
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Flux<DetailingClaim> findAllByStaffUsernameAndActive(String username, Pageable paging) {
        return detailingClaimDao.findByStaffUsernameAndStatus(username, STARTED, paging)
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Flux<DetailingClaim> findByJobId(String jobId) {
        return detailingClaimDao.findByJobId(jobId)
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Flux<DetailingClaim> findByJobIds(List<String> jobIds) {
        return detailingClaimDao.findByJobIds(jobIds)
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Mono<DetailingClaim> findByJobIdAndUnverified(String jobId) {
        return Mono.empty();
    }

    @Override
    public Flux<DetailingClaim> findByJobIdAndNotStatus(String jobId, DetailingStatus notStatus) {
        return null;
    }

    @Override
    public Mono<DetailingClaim> findByJobIdAndClaimed(String jobId) {
        return detailingClaimDao.findByJobIdAndStatus(jobId, UNVERIFIED, STARTED, PAUSED)
                .singleOrEmpty() // this request should not return more than one because of statuses provided
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Mono<DetailingClaim> findByIdAndClaimed(String claimId) {
        return detailingClaimDao.findByIdAndStatus(claimId, UNVERIFIED, STARTED, PAUSED)
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Flux<DetailingClaim> findAllByStaffUsernameAndCompleted(String username, Pageable paging) {
        return detailingClaimDao.findByStaffUsernameAndStatus(username, DetailingStatus.COMPLETED, paging)
                .map(DetailingClaimEntity::toDomainObject);
    }

    @Override
    public Flux<DetailingClaim> findAllByStaffUsernameAndCancelled(String username, Pageable paging) {
        return detailingClaimDao.findByStaffUsernameAndStatus(username, DetailingStatus.CANCELLED, paging)
                .map(DetailingClaimEntity::toDomainObject);
    }

}
