package tech.jmcs.floortech.detailing.infrastructure.persistence.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jmcs.floortech.detailing.domain.model.fileset.FileSet;
import tech.jmcs.floortech.detailing.domain.repository.FileSetRepository;
import tech.jmcs.floortech.detailing.infrastructure.persistence.dao.FileSetDao;
import java.util.List;

@Repository
public class FileSetRepositoryImpl implements FileSetRepository {
    private final FileSetDao fileSetDao;
    @Autowired
    public FileSetRepositoryImpl(FileSetDao fileSetDao) {
        this.fileSetDao = fileSetDao;
    }

    @Override
    public Flux<FileSet> findAll() {
        return null;
    }

    @Override
    public Flux<FileSet> findByIds(List<String> strings) {
        return null;
    }

    @Override
    public Mono<FileSet> findById(String s) {
        return null;
    }

    @Override
    public Mono<FileSet> save(FileSet fileSet) {
        return null;
    }

    @Override
    public Mono<FileSet> delete(String s) {
        return null;
    }
}
