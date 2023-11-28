package tech.jmcs.floortech.detailing.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRow;
import tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRowFacade;

import java.util.Map;
import java.util.Objects;

import static tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRowFacade.*;

public class ExtractedDataRowEntity {
    @Id
    String id;
    Integer row;
    String itemId;
    FileDataEntity parentFileData;
    Map<String, String> data;

    public static ExtractedDataRowEntity fromDomainObject(ExtractedDataRow extractedDataRow) {
        var id = toRowId.apply(extractedDataRow);
        var row = toRow.apply(extractedDataRow);
        var itemId = toItemId.apply(extractedDataRow);
        var parentFileData = FileDataEntity.fromDomainObject(toParent.apply(extractedDataRow));
        var data = toData.apply(extractedDataRow);
        var d = new ExtractedDataRowEntity();
        d.setId(id);
        d.setRow(row);
        d.setItemId(itemId);
        d.setParentFileData(parentFileData);
        d.setData(data);
        return d;
    }

    public ExtractedDataRowEntity() {
    }

    // <editor-fold desc="Getters">
    public String getId() {
        return id;
    }

    public Integer getRow() {
        return row;
    }

    public String getItemId() {
        return itemId;
    }

    public FileDataEntity getParentFileData() {
        return parentFileData;
    }

    public Map<String, String> getData() {
        return data;
    }
    // </editor-fold>

    // <editor-fold desc="Setters">
    public void setId(String id) {
        this.id = id;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setParentFileData(FileDataEntity parentFileData) {
        this.parentFileData = parentFileData;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
    // </editor-fold>

    public ExtractedDataRow toDomainObject() {
        return ExtractedDataRow.builder()
                .id(id)
                .row(row)
                .itemId(itemId)
                .parent(parentFileData.toDomainObject())
                .data(data)
                .build();
    }

    @Override
    public String toString() {
        return "ExtractedDataRowEntity{" +
                "id='" + id + '\'' +
                ", row=" + row +
                ", itemId='" + itemId + '\'' +
//                ", parentFileData=" + parentFileData +
                ", data=" + data +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtractedDataRowEntity that = (ExtractedDataRowEntity) o;
        return id.equals(that.id) && Objects.equals(row, that.row) && Objects.equals(itemId, that.itemId) && Objects.equals(parentFileData, that.parentFileData) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, row, itemId, parentFileData, data);
    }
}
