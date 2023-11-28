package tech.jmcs.floortech.detailing.domain.service.fileprocessing;

import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;
import tech.jmcs.floortech.detailing.domain.service.exception.FileProcessorException;

public interface DetailingDataFileProcessor {
    FileData processFile(DetailingFile detailingFile) throws FileProcessorException;
}
