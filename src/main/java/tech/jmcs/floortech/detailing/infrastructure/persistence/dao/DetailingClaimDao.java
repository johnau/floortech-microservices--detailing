package tech.jmcs.floortech.detailing.infrastructure.persistence.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jmcs.floortech.detailing.domain.configs.DetailingStatus;
import tech.jmcs.floortech.detailing.infrastructure.persistence.entity.DetailingClaimEntity;

import java.util.Date;
import java.util.List;

@Repository
public interface DetailingClaimDao extends ReactiveMongoRepository<DetailingClaimEntity, String> {
    @Query(value = "{jobId: '?0', deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
    Flux<DetailingClaimEntity> findByJobId(String jobId);
    @Query(value = "{jobId: {$in: ?0}, deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
    Flux<DetailingClaimEntity> findByJobIds(List<String> jobIds);

    @Query(value = "{status: '?0', deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
    Flux<DetailingClaimEntity> findByStatus(DetailingStatus status);
    @Query(value = "{status: '?0', deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
    Flux<DetailingClaimEntity> findByStatus(DetailingStatus status, Pageable paging);

    @Query(value = "{id: '?0', status: '?1', deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
    Mono<DetailingClaimEntity> findByIdAndStatus(String jobId, DetailingStatus status);
    @Query(value = "{id: '?0', status: {$in: ?1}, deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
    Mono<DetailingClaimEntity> findByIdAndStatus(String jobId, DetailingStatus... status);

    @Query(value = "{jobId: '?0', status: '?1', deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
    Mono<DetailingClaimEntity> findByJobIdAndStatus(String jobId, DetailingStatus status);
    @Query(value = "{jobId: '?0', status: {$in: ?1}, deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
    Flux<DetailingClaimEntity> findByJobIdAndStatus(String jobId, DetailingStatus... status);

    @Query(value = "{claimedByStaffUsername: '?0', status: '?1', deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
    Flux<DetailingClaimEntity> findByStaffUsernameAndStatus(String username, DetailingStatus status, Pageable paging);

    @Query(value = "{jobId: '?0', claimedByStaffUsername: '?1', status: '?2', deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
    Flux<DetailingClaimEntity> findByCompoundIdAndStatus(String jobId, String username, DetailingStatus status);

//    @Query(value = "{compound_uid: '?0', deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
//    Mono<DetailingClaimEntity> findByCompositeId(String compositeClaimId);


    @Query(value = "{jobId: '?0', claimedByStaffUsername: '?1', createdDate: ?2, deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
    Mono<DetailingClaimEntity> findByCompoundId(String jobId, String username, Date createdDate);

    @Query(value = "{jobId: '?0', claimedByStaffUsername: '?1', deleted: {$nin: [true]}}", fields = "{'deleted': 0}")
    Flux<DetailingClaimEntity> findByCompoundId(String jobId, String username);

    @Query(value = "{deleted: {$nin: [true]}}", fields = "{'deleted': 0}", sort = "{lastModifiedDate : -1}")
    Flux<DetailingClaimEntity> findAllByLastModifiedDescending(Pageable paging);

    @Query(value = "{deleted: {$nin: [true]}}", fields = "{'deleted': 0}", sort = "{createdDate : -1}")
    Flux<DetailingClaimEntity> findAllByCreatedDateDescending(Pageable paging);
}
