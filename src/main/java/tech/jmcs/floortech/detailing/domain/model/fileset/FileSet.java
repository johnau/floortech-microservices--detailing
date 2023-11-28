package tech.jmcs.floortech.detailing.domain.model.fileset;

import tech.jmcs.floortech.detailing.domain.configs.XPath;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;

import java.text.SimpleDateFormat;
import java.util.*;

public class FileSet {
    final String id;
    final String label;
    final List<DetailingFile> files;
    final DetailingClaim jobClaim;
    final XPath zipArchivePath;
    final Date createdDate;

    // <editor-fold desc="Immutable Modifier methods">
    public FileSet withJobClaim(DetailingClaim detailingClaim) {
        return FileSet.builder(id, zipArchivePath)
                .label(label)
                .files(files)
                .jobClaim(detailingClaim)
                .createdDate(createdDate)
                .build();
    }

    public FileSet withFiles(List<DetailingFile> newValueList) {
        Objects.requireNonNull(newValueList);
        return FileSet.builder(id, zipArchivePath)
                .label(label)
                .files(newValueList)
                .jobClaim(jobClaim)
                .createdDate(createdDate)
                .build();
    }
    // </editor-fold>

    // <editor-fold desc="Builder components (Constructor, Builder Class, Static method)">
    public static FileSet createNewFileSet(XPath zipXPath, List<DetailingFile> unprocessedDetailingFiles) {
        return createNewFileSet(UUID.randomUUID().toString(), zipXPath, unprocessedDetailingFiles);
    }

    public static FileSet createNewFileSet(String id, XPath zipXPath, List<DetailingFile> unprocessedDetailingFiles) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(zipXPath);
        Objects.requireNonNull(unprocessedDetailingFiles);
        return FileSet.builder(id, zipXPath)
                .files(unprocessedDetailingFiles)
                .build();
    }

    public static FileSetBuilder builder(String id, XPath zipArchivePath) {
        Objects.requireNonNull(zipArchivePath);
        return new FileSetBuilder(id, zipArchivePath);
    }

    public FileSet(FileSetBuilder builder) {
        this.id = builder.id;
        this.label = builder.label;
        this.files = builder.files != null ? builder.files : new ArrayList<>();
        this.jobClaim = builder.jobClaim;
        this.zipArchivePath = builder.zipArchivePath;
        this.createdDate = builder.createdDate;
    }

    public static class FileSetBuilder {
        private String id;
        private String label;
        private List<DetailingFile> files;
        private DetailingClaim jobClaim;
        private XPath zipArchivePath;
        private Date createdDate;

        public FileSetBuilder(String id, XPath zipArchivePath) {
            var dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
            var date = new Date();
            this.id = id;
            this.label = dateFormat.format(date);
            this.zipArchivePath = zipArchivePath;
            this.createdDate = new Date();
        }

        public FileSetBuilder id(String id) {
            Objects.requireNonNull(id);
            this.id = id;
            return this;
        }

        public FileSetBuilder label(String label) {
            Objects.requireNonNull(label);
            this.label = label;
            return this;
        }

        public FileSetBuilder files(List<DetailingFile> files) {
            this.files = files;
            return this;
        }

        public FileSetBuilder jobClaim(DetailingClaim jobClaim) {
            this.jobClaim = jobClaim;
            return this;
        }

        public FileSetBuilder zipArchivePath(XPath zipArchivePath) {
            Objects.requireNonNull(zipArchivePath);
            this.zipArchivePath = zipArchivePath;
            return this;
        }

        public FileSetBuilder createdDate(Date createdDate) {
            Objects.requireNonNull(createdDate);
            this.createdDate = createdDate;
            return this;
        }

        public FileSet build() {
            Objects.requireNonNull(this.id);
            Objects.requireNonNull(this.label);
            Objects.requireNonNull(this.zipArchivePath);
            Objects.requireNonNull(this.createdDate);
            return new FileSet(this);
        }
    }
    // </editor-fold>

    @Override
    public String toString() {
        return "FileSet{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", files=" + files +
                ", zipArchivePath=" + zipArchivePath +
                ", createdDate=" + createdDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileSet fileSet = (FileSet) o;
        return id.equals(fileSet.id) && label.equals(fileSet.label) && Objects.equals(files, fileSet.files) && jobClaim.equals(fileSet.jobClaim) && zipArchivePath.equals(fileSet.zipArchivePath) && createdDate.equals(fileSet.createdDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, files, jobClaim, zipArchivePath, createdDate);
    }
}