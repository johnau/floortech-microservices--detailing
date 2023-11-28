package tech.jmcs.floortech.detailing.domain.model.files;

import tech.jmcs.floortech.detailing.domain.configs.XPath;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;

import java.util.Date;
import java.util.function.Function;

/**
 * Accessor Facade for DetailingClaim, should not be used by Service layer (except *possibly* for id)
 * ID logic changing to composite ID and will use a different method in the DetailingClaim object
 *
 * Facade/ accessor - Responsible for ensuring the immutability of the underlying class DetailingClaim
 * Non-primitive types are cloned/deep-copied
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
