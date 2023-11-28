package tech.jmcs.floortech.detailing.domain.model.filedata;

import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;

import java.util.Map;
import java.util.function.Function;

/**
 * Accessor Facade for DetailingClaim, should not be used by Service layer (except *possibly* for id)
 * ID logic changing to composite ID and will use a different method in the DetailingClaim object
 *
 * Facade/ accessor - Responsible for ensuring the immutability of the underlying class DetailingClaim
 * Non-primitive types are cloned/deep-copied
 */
public interface FileDataFacade {
    Function<FileData, String> toFileDataId = fileData -> fileData.id;
    Function<FileData, Map<Integer, String>> toLines = fileData -> fileData.lines;
    Function<FileData, Map<String, ExtractedDataRow>> toExtractedData = fileData -> fileData.extractedData;
    Function<FileData, DetailingFile> toDetailingFile = fileData -> fileData.parent;
}
