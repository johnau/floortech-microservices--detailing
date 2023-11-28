package tech.jmcs.floortech.detailing.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import tech.jmcs.floortech.detailing.domain.configs.XPath;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;

import java.util.Date;
import java.util.Objects;

import static tech.jmcs.floortech.detailing.domain.model.files.DetailingFileFacade.*;

public class DetailingFileEntity {
    @Id
    String id;
    XPath path;
    String extension;
    String filename;
    Boolean processed;
    Long fileSize;
    String mimetype;
    XPath parentFolder;
    String label;
    FileDataEntity fileData;
    Date createdDate;

    public static DetailingFileEntity fromDomainObject(DetailingFile detailingFile) {
        var id = toFileId.apply(detailingFile);
        var path = toPath.apply(detailingFile);
        var extension = toExtension.apply(detailingFile);
        var filename = toFilename.apply(detailingFile);
        var isProcessed = toIsProcessed.apply(detailingFile);
        var fileSize = toFileSize.apply(detailingFile);
        var mimeType = toMimeType.apply(detailingFile);
        var parentFolder = toParentFolder.apply(detailingFile);
        var label = toLabel.apply(detailingFile);
        var createdData = toCreationDate.apply(detailingFile);
        var _fileData = toFileData.apply(detailingFile);
        FileDataEntity fileData = null;
        if (_fileData != null) {
            fileData = FileDataEntity.fromDomainObject(toFileData.apply(detailingFile));
        }

        var d = new DetailingFileEntity();
        d.setId(id);
        d.setPath(path);
        d.setExtension(extension);
        d.setFilename(filename);
        d.setProcessed(isProcessed);
        d.setFileSize(fileSize);
        d.setMimetype(mimeType);
        d.setParentFolder(parentFolder);
        d.setLabel(label);
        d.setFileData(fileData);
        d.setCreatedDate(createdData);
        return d;
    }

    public DetailingFileEntity() {
    }

    // <editor-fold desc="Getters">
    public String getId() {
        return id;
    }

    public XPath getPath() {
        return path;
    }

    public String getExtension() {
        return extension;
    }

    public String getFilename() {
        return filename;
    }
    public Boolean isProcessed() {
        return processed;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getMimetype() {
        return mimetype;
    }

    public XPath getParentFolder() {
        return parentFolder;
    }

    public String getLabel() {
        return label;
    }

    public FileDataEntity getFileData() {
        return fileData;
    }

    public Date getCreatedDate() {
        return createdDate;
    }
    // </editor-fold>

    // <editor-fold desc="Setters">
    public void setId(String id) {
        this.id = id;
    }

    public void setPath(XPath path) {
        this.path = path;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public void setParentFolder(XPath parentFolder) {
        this.parentFolder = parentFolder;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setFileData(FileDataEntity fileData) {
        this.fileData = fileData;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
    // </editor-fold>

    public DetailingFile toDomainObject() {
        FileData fileDataDomain = null;
        if (fileData != null) {
            fileDataDomain = fileData.toDomainObject();
        }

        return DetailingFile.builder(label, path, filename, extension)
                .id(id)
                .isProcessed(processed)
                .fileSize(fileSize)
                .mimetype(mimetype)
                .parentFolder(parentFolder)
                .creationDate(createdDate)
                .fileData(fileDataDomain)
                .build();
    }

    @Override
    public String toString() {
        return "DetailingFileEntity{" +
                "id='" + id + '\'' +
                ", path=" + path +
                ", extension='" + extension + '\'' +
                ", filename='" + filename + '\'' +
                ", isProcessed=" + processed +
                ", fileSize=" + fileSize +
                ", mimetype='" + mimetype + '\'' +
                ", parentFolder=" + parentFolder +
                ", label='" + label + '\'' +
                ", fileData=" + fileData +
                ", createdDate=" + createdDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetailingFileEntity that = (DetailingFileEntity) o;
        return id.equals(that.id) && path.equals(that.path) && extension.equals(that.extension) && filename.equals(that.filename) && processed.equals(that.processed) && Objects.equals(fileSize, that.fileSize) && Objects.equals(mimetype, that.mimetype) && Objects.equals(parentFolder, that.parentFolder) && label.equals(that.label) && Objects.equals(fileData, that.fileData) && createdDate.equals(that.createdDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, path, extension, filename, processed, fileSize, mimetype, parentFolder, label, fileData, createdDate);
    }
}
