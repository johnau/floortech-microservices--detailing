package tech.jmcs.floortech.detailing.domain.model.files;

import tech.jmcs.floortech.detailing.domain.configs.XPath;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;

import java.util.Date;
import java.util.function.Function;

/**
 * Facade/Data accessor - Ensures immutability of domain objects:
 * - Non-primitive types are cloned/deep-copied
 */
public interface DetailingFileFacade {
    Function<DetailingFile, String> toFileId = detailingFile -> detailingFile.id;
    Function<DetailingFile, XPath> toPath = detailingFile -> detailingFile.path;
    Function<DetailingFile, String> toExtension = detailingFile -> detailingFile.extension;
    Function<DetailingFile, String> toFilename = detailingFile -> detailingFile.filename;
    Function<DetailingFile, Long> toFileSize = detailingFile -> detailingFile.fileSize;
    Function<DetailingFile, String> toMimeType = detailingFile -> detailingFile.mimetype;
    Function<DetailingFile, XPath> toParentFolder = detailingFile -> detailingFile.parentFolder;
    Function<DetailingFile, String> toLabel = detailingFile -> detailingFile.label;
    Function<DetailingFile, Date> toCreationDate = detailingFile -> detailingFile.creationDate;
    Function<DetailingFile, FileData> toFileData = detailingFile -> detailingFile.fileData;
    Function<DetailingFile, Boolean> toIsProcessed = detailingFile -> detailingFile.isProcessed;
}
