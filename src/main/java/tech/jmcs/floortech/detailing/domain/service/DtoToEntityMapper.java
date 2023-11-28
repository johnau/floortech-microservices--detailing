package tech.jmcs.floortech.detailing.domain.service;

public interface DtoToEntityMapper<D, E> {
    E map(D dto);
}
