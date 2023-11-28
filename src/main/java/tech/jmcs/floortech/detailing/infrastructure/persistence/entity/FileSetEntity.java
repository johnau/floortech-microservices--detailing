package tech.jmcs.floortech.detailing.infrastructure.persistence.entity;

import tech.jmcs.floortech.detailing.domain.configs.XPath;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;
import tech.jmcs.floortech.detailing.domain.model.fileset.FileSet;
import tech.jmcs.floortech.detailing.domain.model.fileset.FileSetFacade;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static tech.jmcs.floortech.detailing.domain.model.fileset.FileSetFacade.*;

public class FileSetEntity { //extends AuditBase {
    String id;
    String label;
    List<DetailingFileEntity> files;
    DetailingClaimEntity parentClaim;
    XPath zipArchiveXPath;
    Date createdDate;

    public static FileSetEntity fromDomainObject(FileSet fileSet) {
        var id = toFileSetId.apply(fileSet);
        var label = toLabel.apply(fileSet);
        var createdDate = toCreatedDate.apply(fileSet);
        var files = toFiles.apply(fileSet);
        List<DetailingFileEntity> fileEntities = null;
        if (files != null) {
            fileEntities = files.stream()
                    .map(detailingFile -> DetailingFileEntity.fromDomainObject(detailingFile))
                    .collect(Collectors.toList());
        }
        var detailingClaim = toDetailingClaim.apply(fileSet);
        DetailingClaimEntity parentClaim = null;
        if (detailingClaim != null) {
            parentClaim = DetailingClaimEntity.fromDomainObject(detailingClaim);
        }
        var zipArchivePath = toZipArchivePath.apply(fileSet);
        var f = new FileSetEntity();
        f.setId(id);
        f.setLabel(label);
        f.setFiles(fileEntities);
        f.setParentClaim(parentClaim);
        f.setZipArchiveXPath(zipArchivePath);
        f.setCreatedDate(createdDate);
        return f;
    }

    public FileSetEntity() {
    }

    // <editor-fold desc="Getters">
    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public List<DetailingFileEntity> getFiles() {
        return files;
    }

    public DetailingClaimEntity getParentClaim() {
        return parentClaim;
    }

    public XPath getZipArchiveXPath() {
        return zipArchiveXPath;
    }

    public Date getCreatedDate() {
        return createdDate;
    }
    // </editor-fold>

    // <editor-fold desc="Setters">
    public void setId(String id) {
        this.id = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setFiles(List<DetailingFileEntity> files) {
        this.files = files;
    }

    public void setParentClaim(DetailingClaimEntity parentClaim) {
        this.parentClaim = parentClaim;
    }

    public void setZipArchiveXPath(XPath zipArchiveXPath) {
        this.zipArchiveXPath = zipArchiveXPath;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    // </editor-fold>

    public FileSet toDomainObject() {
        List<DetailingFile> filesDomain = null;
        if (files != null)
            filesDomain = files.stream()
                .map(file -> file.toDomainObject())
                .collect(Collectors.toList());
        return FileSet.builder(id, zipArchiveXPath)
                .label(label)
                .files(filesDomain)
                .jobClaim(null)
                .createdDate(createdDate)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileSetEntity that = (FileSetEntity) o;
        return id.equals(that.id) && label.equals(that.label) && Objects.equals(files, that.files) && parentClaim.equals(that.parentClaim) && zipArchiveXPath.equals(that.zipArchiveXPath) && createdDate.equals(that.createdDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, files, parentClaim, zipArchiveXPath, createdDate);
    }
}
