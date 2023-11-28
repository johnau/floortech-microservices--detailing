package tech.jmcs.floortech.detailing.domain.configs;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CrudRepository<T, ID> {

    Flux<T> findAll();
    Flux<T> findByIds(List<ID> ids);
    Mono<T> findById(ID id);
    Mono<T> save(T t);
    Mono<T> delete(ID id);

}