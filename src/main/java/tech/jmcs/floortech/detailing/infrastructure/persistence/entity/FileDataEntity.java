package tech.jmcs.floortech.detailing.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRow;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileDataFacade;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static tech.jmcs.floortech.detailing.domain.model.filedata.FileDataFacade.*;

public class FileDataEntity {
    @Id
    String id;
    Map<Integer, String> lines;
    Map<String, ExtractedDataRowEntity> extractedData;
    DetailingFileEntity parentFile;

    public static FileDataEntity fromDomainObject(FileData fileData) {
        var id = toFileDataId.apply(fileData);
        var lines = toLines.apply(fileData);
        var _extractedData = toExtractedData.apply(fileData);
        Map<String, ExtractedDataRowEntity> extractedData = Map.of();
        if (_extractedData != null) {
            extractedData = _extractedData.entrySet().stream()
                    .map(entry -> {
                        var key = entry.getKey();
                        var value = ExtractedDataRowEntity.fromDomainObject(entry.getValue());
                        return Map.entry(key, value);
                    })
                    .collect(Collectors.toMap(
                            e -> (String) e.getKey(),
                            e -> (ExtractedDataRowEntity) e.getValue()
                    ));
        }
        var parentFile = DetailingFileEntity.fromDomainObject(toDetailingFile.apply(fileData));
        var d = new FileDataEntity();
        d.setId(id);
        d.setLines(lines);
        d.setExtractedData(extractedData);
        d.setParentFile(parentFile);
        return d;
    }

    public FileDataEntity() {
    }

    // <editor-fold desc="Getters">
    public String getId() {
        return id;
    }

    public Map<Integer, String> getLines() {
        return lines;
    }

    public Map<String, ExtractedDataRowEntity> getExtractedData() {
        return extractedData;
    }

    public DetailingFileEntity getParentFile() {
        return parentFile;
    }
    // </editor-fold>

    // <editor-fold desc="Setters">
    public void setId(String id) {
        this.id = id;
    }

    public void setLines(Map<Integer, String> lines) {
        this.lines = lines;
    }

    public void setExtractedData(Map<String, ExtractedDataRowEntity> extractedData) {
        this.extractedData = extractedData;
    }

    public void setParentFile(DetailingFileEntity parentFile) {
        this.parentFile = parentFile;
    }
    // </editor-fold>

    public FileData toDomainObject() {
        var extractedDataDomain = extractedData.entrySet().stream()
                .map(entry -> {
                    var key = entry.getKey();
                    var value = entry.getValue().toDomainObject();
                    return Map.entry(key, value);
                })
                .collect(Collectors.toMap(
                    e -> (String) e.getKey(),
                    e -> (ExtractedDataRow) e.getValue()
                ));
        return FileData.builder(id, parentFile.toDomainObject())
                .lines(lines)
                .extractedData(extractedDataDomain)
                .build();
    }

    @Override
    public String toString() {
        return "FileDataEntity{" +
                "id='" + id + '\'' +
                ", lines=" + lines +
                ", extractedData=" + extractedData +
//                ", parentFile=" + parentFile +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileDataEntity that = (FileDataEntity) o;
        return id.equals(that.id) && Objects.equals(lines, that.lines) && Objects.equals(extractedData, that.extractedData) && Objects.equals(parentFile, that.parentFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lines, extractedData, parentFile);
    }
}
