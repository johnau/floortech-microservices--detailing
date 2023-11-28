package tech.jmcs.floortech.detailing.domain.model.filedata;


import java.util.Map;
import java.util.Objects;

public class ExtractedDataRow {
    final String id;
    final Integer row;
    final String itemId;
    final FileData parent;
    final Map<String, String> data;

    // <editor-fold desc="Immutable Modifier methods">
    public ExtractedDataRow withRow(Integer newValue) {
        return ExtractedDataRow.builder()
                .row(newValue)
                .itemId(itemId)
                .parent(parent)
                .data(data)
                .build();
    }

    public ExtractedDataRow withItemId(String newValue) {
        return ExtractedDataRow.builder()
                .row(row)
                .itemId(newValue)
                .parent(parent)
                .data(data)
                .build();
    }

    public ExtractedDataRow withParent(tech.jmcs.floortech.detailing.domain.model.filedata.FileData newValue) {
        return ExtractedDataRow.builder()
                .row(row)
                .itemId(itemId)
                .parent(newValue)
                .data(data)
                .build();
    }

    public ExtractedDataRow withData(Map<String, String> newValue) {
        return ExtractedDataRow.builder()
                .row(row)
                .itemId(itemId)
                .parent(parent)
                .data(newValue)
                .build();
    }
    // </editor-fold>

    // <editor-fold desc="Builder components (Constructor, Builder Class, Static method)">
    public static ExtractedDataRowBuilder builder() {
        return new ExtractedDataRowBuilder();
    }

    public ExtractedDataRow(ExtractedDataRowBuilder builder) {
        this.id = builder.id;
        this.row = builder.row;
        this.itemId = builder.itemId;
        this.parent = builder.parent;
        this.data = builder.data;
    }

    public static class ExtractedDataRowBuilder {
        private String id;
        private Integer row;
        private String itemId;
        private FileData parent;
        private Map<String, String> data;
        public ExtractedDataRowBuilder() {
        }

        public ExtractedDataRowBuilder id(String id) {
            this.id = id;
            return this;
        }

        public ExtractedDataRowBuilder row(Integer row) {
            this.row = row;
            return this;
        }

        public ExtractedDataRowBuilder itemId(String itemId) {
            this.itemId = itemId;
            return this;
        }

        public ExtractedDataRowBuilder parent(tech.jmcs.floortech.detailing.domain.model.filedata.FileData parent) {
            this.parent = parent;
            return this;
        }

        public ExtractedDataRowBuilder data(Map<String, String> data) {
            this.data = data;
            return this;
        }

        public ExtractedDataRow build() {
            return new ExtractedDataRow(this);
        }
    }
    // </editor-fold>

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtractedDataRow that = (ExtractedDataRow) o;
        return id.equals(that.id) && row.equals(that.row) && Objects.equals(itemId, that.itemId) && Objects.equals(parent, that.parent) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, row, itemId, parent, data);
    }

    @Override
    public String toString() {
        return "ExtractedDataRow{" +
                "id='" + id + '\'' +
                ", row=" + row +
                ", itemId='" + itemId + '\'' +
                ", parent=" + parent +
                ", data=" + data +
                '}';
    }
}
