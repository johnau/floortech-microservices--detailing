package tech.jmcs.floortech.detailing.domain.service.fileprocessing;

import tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRow;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;

import java.util.List;
import java.util.Map;

public interface TableProcessor {

    boolean isRecognized(String title, String[] columns);

    Map<String, ExtractedDataRow> processData(FileData fileData, String title, String[] columns, List<String[]> dataRows);

}
