package tech.jmcs.floortech.detailing.domain.model.filedata;

import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FileData {
    final String id;
    final Map<Integer, String> lines;
    final Map<String, tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRow> extractedData;
    final DetailingFile parent;

    // <editor-fold desc="Immutable Modifier methods">
    public FileData withExtractedData(Map<String, tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRow> newValue) {
        return FileData.builder(id, parent)
                .lines(lines)
                .extractedData(newValue)
                .build();
    }
    // </editor-fold>

    // <editor-fold desc="Builder components (Constructor, Builder Class, Static method)">
    public static FileData createEmpty(DetailingFile detailingFile) {
        var id = UUID.randomUUID().toString();
        return FileData.builder(id, detailingFile)
                .build();
    }

    public static FileDataBuilder builder(String id, DetailingFile parent) {
        return new FileDataBuilder(id, parent);
    }

    public FileData(FileDataBuilder builder) {
        this.id = builder.id;
        this.lines = builder.lines != null ? builder.lines : new HashMap<>();
        this.extractedData = builder.extractedData != null ? builder.extractedData : new HashMap<>();
        this.parent = builder.parent;
    }

    public static class FileDataBuilder {
        private String id;
        private Map<Integer, String> lines;
        private Map<String, tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRow> extractedData;
        private DetailingFile parent;
        public FileDataBuilder(String id, DetailingFile parent) {
            Objects.requireNonNull(id);
            Objects.requireNonNull(parent);
            this.id = id;
            this.parent = parent;
        }

        public FileDataBuilder id(String id) {
            this.id = id;
            return this;
        }

        public FileDataBuilder lines(Map<Integer, String> lines) {
            this.lines = lines;
            return this;
        }

        public FileDataBuilder extractedData(Map<String, tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRow> extractedData) {
            this.extractedData = extractedData;
            return this;
        }

        public FileDataBuilder parent(DetailingFile parent) {
            this.parent = parent;
            return this;
        }

        public FileData build() {
            return new FileData(this);
        }
    }
    // </editor-fold>

    @Override
    public String toString() {
        return "FileData{" +
                "id='" + id + '\'' +
                ", lines=" + lines +
                ", extractedData=" + extractedData +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileData fileData = (FileData) o;
        return id.equals(fileData.id) && Objects.equals(lines, fileData.lines) && Objects.equals(extractedData, fileData.extractedData) && parent.equals(fileData.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lines, extractedData, parent);
    }
}
