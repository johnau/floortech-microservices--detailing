package tech.jmcs.floortech.detailing.infrastructure.persistence.dao;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import tech.jmcs.floortech.detailing.infrastructure.persistence.entity.FileSetEntity;

public interface FileSetDao extends ReactiveMongoRepository<FileSetEntity, String> {

}
