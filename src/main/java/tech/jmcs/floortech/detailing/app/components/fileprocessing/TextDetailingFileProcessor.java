package tech.jmcs.floortech.detailing.app.components.fileprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.jmcs.floortech.detailing.domain.service.exception.FileProcessorException;
import tech.jmcs.floortech.detailing.app.components.fileprocessing.processors.BeamListTextTableProcessor;
import tech.jmcs.floortech.detailing.app.components.fileprocessing.processors.SheetListTextTableProcessor;
import tech.jmcs.floortech.detailing.app.components.fileprocessing.processors.TrussListTextTableProcessor;
import tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRow;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFileFacade;
import tech.jmcs.floortech.detailing.domain.service.fileprocessing.DetailingDataFileProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static tech.jmcs.floortech.detailing.domain.model.files.DetailingFileFacade.*;

public class TextDetailingFileProcessor implements DetailingDataFileProcessor {
    final static Logger log = LoggerFactory.getLogger(TextDetailingFileProcessor.class);
    final Path storagePath;
    public TextDetailingFileProcessor(Path storagePath) {
        this.storagePath = storagePath;
    }

    @Override
    public FileData processFile(DetailingFile detailingFile) throws FileProcessorException {
        var fileData = toFileData.apply(detailingFile);
        if (fileData == null) {
            fileData = FileData.createEmpty(detailingFile);
        }
        var _path = toPath.apply(detailingFile);
        var filePath = Paths.get(_path.path());
        var relativePath = "";
        if (_path.isRelative()) {
            filePath = Paths.get(storagePath.toString(), _path.path());
        }

        List<String> rawRowData;
        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            rawRowData = br.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new FileProcessorException("An expected file could not be found or accessed: " + e.getMessage());
        }

        TextFileData _d = iterateData(rawRowData);

        var beamListTextProcessor = new BeamListTextTableProcessor();
        var trussListTextProcessor = new TrussListTextTableProcessor();
        var sheetListTextProcessor = new SheetListTextTableProcessor();
        Map<String, ExtractedDataRow> processedDataMap = null;
        if (beamListTextProcessor.isRecognized(_d.title(), _d.columns())) {
            processedDataMap = beamListTextProcessor.processData(fileData, _d.title(), _d.columns(), _d.dataRows());
        } else if (trussListTextProcessor.isRecognized(_d.title(), _d.columns())) {
            processedDataMap = trussListTextProcessor.processData(fileData, _d.title(), _d.columns(), _d.dataRows());
        } else if (sheetListTextProcessor.isRecognized(_d.title(), _d.columns())) {
            processedDataMap = sheetListTextProcessor.processData(fileData, _d.title(), _d.columns(), _d.dataRows());
        } else {
            log.info("Text file not recognized by any processors");
            throw new FileProcessorException("Data file not recognized");
        }

        return fileData.withExtractedData(processedDataMap);
    }

    private TextFileData iterateData(List<String> rawRowData) {
        var iterator = rawRowData.iterator();
        var title = "";
        var columns = new String[]{};
        var dataRows = new ArrayList<String[]>();
        Map<Integer, String> rawDataMap = new HashMap<Integer, String>();

        int lineNumber = 0;
        while (iterator.hasNext()) {
            String nextValue = iterator.next();
            rawDataMap.put(lineNumber, nextValue);
            if (lineNumber == 0) title = nextValue.trim();
            else if (lineNumber == 1) columns = nextValue.trim().split("\t");
            else dataRows.add(nextValue.trim().split("\t"));

            lineNumber++;
        }

        return new TextFileData(title, columns, dataRows, rawDataMap);
    }


}
