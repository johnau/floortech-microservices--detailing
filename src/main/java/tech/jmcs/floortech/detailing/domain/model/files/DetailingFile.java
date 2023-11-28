package tech.jmcs.floortech.detailing.domain.model.files;

import tech.jmcs.floortech.common.helper.StringHelper;
import tech.jmcs.floortech.detailing.domain.configs.XPath;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DetailingFile {
    final String id;
    final String label;
    final XPath path;
    final String extension;
    final String filename;
    final Boolean isProcessed;
    final Long fileSize;
    final String mimetype;
    final XPath parentFolder;
    final Date creationDate;
    final FileData fileData;

    // <editor-fold desc="Immutable Modifier methods">
    public DetailingFile withIsProcessed(Boolean newValue) {
        return DetailingFile.builder(label, path, filename, extension)
                .id(id)
                .isProcessed(newValue)
                .fileSize(fileSize)
                .mimetype(mimetype)
                .parentFolder(parentFolder)
                .creationDate(creationDate)
                .fileData(fileData)
                .build();
    }

    public DetailingFile withFileData(FileData newValue) {
        return DetailingFile.builder(label, path, filename, extension)
                .id(id)
                .isProcessed(isProcessed)
                .fileSize(fileSize)
                .mimetype(mimetype)
                .parentFolder(parentFolder)
                .creationDate(creationDate)
                .fileData(newValue)
                .build();
    }

    public DetailingFile withMimeType(String newValue) {
        return DetailingFile.builder(label, path, filename, extension)
                .id(id)
                .isProcessed(isProcessed)
                .fileSize(fileSize)
                .mimetype(newValue)
                .parentFolder(parentFolder)
                .creationDate(creationDate)
                .fileData(fileData)
                .build();
    }

    public DetailingFile withFileSize(long newValue) {
        return DetailingFile.builder(label, path, filename, extension)
                .id(id)
                .isProcessed(isProcessed)
                .fileSize(newValue)
                .mimetype(mimetype)
                .parentFolder(parentFolder)
                .creationDate(creationDate)
                .fileData(fileData)
                .build();
    }
    // </editor-fold>

    // <editor-fold desc="Builder components (Constructor, Builder Class, Static method)">
    public static DetailingFile createUnprocessedDetailingFile(XPath path, String label) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(label);
        var _path = Paths.get(path.path());
        var filename = _path.getFileName().toString();
        var extension = StringHelper.getFileExtension(filename).orElse("");
        return DetailingFile.builder(label, path, filename, extension)
                .creationDate(new Date())
                .build();
    }

    public static List<DetailingFile> createUnprocessedDetailingFileList(List<Path> relativePaths) {
        return relativePaths.stream()
                .map(relativePath -> XPath.relative(relativePath.toString()))
                .map(xPath -> DetailingFile.createUnprocessedDetailingFile(xPath, xPath.toFileName()))
                .collect(Collectors.toList());
    }

    public static DetailingFileBuilder builder(String label, XPath path, String filename, String extension) {
        Objects.requireNonNull(label);
        Objects.requireNonNull(path);
        Objects.requireNonNull(filename);
        Objects.requireNonNull(extension);
        return new DetailingFileBuilder(label, path, filename, extension, false);
    }

    public DetailingFile(DetailingFileBuilder builder) {
        this.id = builder.id;
        this.label = builder.label;
        this.path = builder.path;
        this.extension = builder.extension;
        this.filename = builder.filename;
        this.isProcessed = builder.isProcessed;
        this.fileSize = builder.fileSize;
        this.mimetype = builder.mimetype;
        this.parentFolder = builder.parentFolder;
        this.creationDate = builder.creationDate;
        this.fileData = builder.fileData;
    }

    public static class DetailingFileBuilder {
        private String id;
        private String label;
        private XPath path;
        private String extension;
        private String filename;
        private Boolean isProcessed;
        private Long fileSize;
        private String mimetype;
        private XPath parentFolder;
        private Date creationDate;
        private FileData fileData;

        public DetailingFileBuilder(String label, XPath path, String extension, String filename, Boolean isProcessed) {
            Objects.requireNonNull(label);
            Objects.requireNonNull(path);
            Objects.requireNonNull(extension);
            Objects.requireNonNull(filename);
            Objects.requireNonNull(isProcessed);
            this.label = label;
            this.path = path;
            this.extension = extension;
            this.filename = filename;
            this.isProcessed = isProcessed;
        }

        public DetailingFileBuilder id(String id) {
            this.id = id;
            return this;
        }

        public DetailingFileBuilder label(String label) {
            this.label = label;
            return this;
        }

        public DetailingFileBuilder path(XPath path) {
            this.path = path;
            return this;
        }

        public DetailingFileBuilder extension(String extension) {
            this.extension = extension;
            return this;
        }

        public DetailingFileBuilder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public DetailingFileBuilder isProcessed(Boolean processed) {
            isProcessed = processed;
            return this;
        }

        public DetailingFileBuilder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public DetailingFileBuilder mimetype(String mimetype) {
            this.mimetype = mimetype;
            return this;
        }

        public DetailingFileBuilder parentFolder(XPath parentFolder) {
            this.parentFolder = parentFolder;
            return this;
        }

        public DetailingFileBuilder creationDate(Date creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public DetailingFileBuilder fileData(FileData fileData) {
            this.fileData = fileData;
            return this;
        }

        public DetailingFile build() {
            Objects.requireNonNull(this.label);
            Objects.requireNonNull(this.path);
            return new DetailingFile(this);
        }
    }
    // </editor-fold>

    @Override
    public String toString() {
        return "DetailingFile{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", path=" + path +
                ", extension='" + extension + '\'' +
                ", filename='" + filename + '\'' +
                ", isProcessed=" + isProcessed +
                ", fileSize=" + fileSize +
                ", mimetype='" + mimetype + '\'' +
                ", parentFolder=" + parentFolder +
                ", creationDate=" + creationDate +
                ", fileData=" + fileData +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetailingFile that = (DetailingFile) o;
        return id.equals(that.id) && label.equals(that.label) && path.equals(that.path) && extension.equals(that.extension) && filename.equals(that.filename) && isProcessed.equals(that.isProcessed) && Objects.equals(fileSize, that.fileSize) && Objects.equals(mimetype, that.mimetype) && Objects.equals(parentFolder, that.parentFolder) && Objects.equals(creationDate, that.creationDate) && Objects.equals(fileData, that.fileData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, path, extension, filename, isProcessed, fileSize, mimetype, parentFolder, creationDate, fileData);
    }
}
