package tech.jmcs.floortech.detailing.app.components;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.jmcs.floortech.detailing.domain.service.exception.FileProcessorException;
import tech.jmcs.floortech.detailing.domain.service.fileprocessing.DetailingDataFileProcessor;
import tech.jmcs.floortech.detailing.app.components.fileprocessing.DetailingDataFileProcessorFactory;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFileFacade;
import tech.jmcs.floortech.detailing.domain.service.fileprocessing.FileProcessor;

import java.nio.file.Path;

import static tech.jmcs.floortech.detailing.domain.model.files.DetailingFileFacade.*;

@Component("archicadDataFileProcessingService")
public class ArchicadDataFileProcessorImpl implements FileProcessor {
    static final Logger log = LoggerFactory.getLogger(ArchicadDataFileProcessorImpl.class);
    static final String[] ACCEPTED_TYPES = {"XLS", "XLSX", "TXT"};
    final DetailingDataFileProcessorFactory detailingDataFileProcessorFactory;
    @Autowired
    public ArchicadDataFileProcessorImpl(DetailingDataFileProcessorFactory detailingDataFileProcessorFactory) {
        this.detailingDataFileProcessorFactory = detailingDataFileProcessorFactory;
    }

    @Override
    public FileData processFileData(DetailingFile detailingFile, Path storageBasePath) throws FileProcessorException {
        var mimetype = toMimeType.apply(detailingFile);
        var extension = toExtension.apply(detailingFile);
        if (extension == null || !ArrayUtils.contains(ACCEPTED_TYPES, extension.toUpperCase())) {
            throw new FileProcessorException("Unsupported file extension: " + extension);
        }

        DetailingDataFileProcessor processor = detailingDataFileProcessorFactory.createDetailingDataFileProcessor(detailingFile, storageBasePath);
        var data = processor.processFile(detailingFile);
        if (data == null) {
            throw new FileProcessorException("Unable to process file: " + toPath.apply(detailingFile));
        }

        return data;
    }

}
