package tech.jmcs.floortech.detailing.app.dto;

import tech.jmcs.floortech.detailing.domain.configs.XPath;

import java.util.Date;

public record GetDetailingFileDto(
    String id,
    XPath filePath,
    String extension,
    String filename,
    Long fileSize,
    String mimetype,
    XPath parentFolder,
    String label,
    Date creationDate,
    GetFileDataDto fileData
) {
}
