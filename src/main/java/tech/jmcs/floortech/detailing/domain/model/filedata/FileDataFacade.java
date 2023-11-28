package tech.jmcs.floortech.detailing.domain.model.filedata;

import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;

import java.util.Map;
import java.util.function.Function;

/**
 * Facade/Data accessor - Ensures immutability of domain objects:
 * - Non-primitive types are cloned/deep-copied
 */
public interface FileDataFacade {
    Function<FileData, String> toFileDataId = fileData -> fileData.id;
    Function<FileData, Map<Integer, String>> toLines = fileData -> fileData.lines;
    Function<FileData, Map<String, ExtractedDataRow>> toExtractedData = fileData -> fileData.extractedData;
    Function<FileData, DetailingFile> toDetailingFile = fileData -> fileData.parent;
}
