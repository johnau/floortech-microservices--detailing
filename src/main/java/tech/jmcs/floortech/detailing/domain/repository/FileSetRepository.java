package tech.jmcs.floortech.detailing.domain.repository;


import tech.jmcs.floortech.detailing.domain.configs.CrudRepository;
import tech.jmcs.floortech.detailing.domain.model.fileset.FileSet;

public interface FileSetRepository extends CrudRepository<FileSet, String> {
//    Mono<FileSet> getFileSetByJob
}
