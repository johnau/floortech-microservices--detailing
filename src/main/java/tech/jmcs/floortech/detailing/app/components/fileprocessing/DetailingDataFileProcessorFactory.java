package tech.jmcs.floortech.detailing.app.components.fileprocessing;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;
import tech.jmcs.floortech.detailing.domain.service.fileprocessing.DetailingDataFileProcessor;

import java.nio.file.Path;

import static tech.jmcs.floortech.detailing.domain.model.files.DetailingFileFacade.*;

@Service
public class DetailingDataFileProcessorFactory {
    static final Logger log = LoggerFactory.getLogger(DetailingDataFileProcessorFactory.class);

    public DetailingDataFileProcessorFactory() {
    }

    public DetailingDataFileProcessor createDetailingDataFileProcessor(DetailingFile detailingFile, Path storageBasePath) {
        var extension = toExtension.apply(detailingFile);
        var mimetype = toMimeType.apply(detailingFile);
        // TODO: Check mimetype

        var extensionUcase = extension.toUpperCase();
        switch (extensionUcase) {
            case "TXT":
                return createTextDetailingFileProcessor(storageBasePath);
            case "XLS":
                throw new NotImplementedException("XLS handling not yet implemented");
            default:
                throw new NotImplementedException("No other handling available");
        }
    }

    private TextDetailingFileProcessor createTextDetailingFileProcessor(Path storagePath) {
        return new TextDetailingFileProcessor(storagePath);
    }
}
