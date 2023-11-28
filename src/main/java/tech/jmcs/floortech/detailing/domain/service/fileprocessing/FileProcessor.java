package tech.jmcs.floortech.detailing.domain.service.fileprocessing;

import org.springframework.stereotype.Component;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;
import tech.jmcs.floortech.detailing.domain.service.exception.FileProcessorException;

import java.nio.file.Path;

@Component
public interface FileProcessor {
    FileData processFileData(DetailingFile detailingFile, Path storageBasePath) throws FileProcessorException;
}
