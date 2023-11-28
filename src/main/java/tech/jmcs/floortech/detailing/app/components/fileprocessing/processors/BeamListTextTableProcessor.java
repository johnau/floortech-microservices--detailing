package tech.jmcs.floortech.detailing.app.components.fileprocessing.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.jmcs.floortech.detailing.app.components.fileprocessing.config.ArchiCadBeamListingTextFile;
import tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRow;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;
import tech.jmcs.floortech.detailing.domain.service.fileprocessing.TableProcessor;

import java.util.*;
import java.util.stream.Collectors;

public class BeamListTextTableProcessor implements TableProcessor {
    static final Logger log = LoggerFactory.getLogger(BeamListTextTableProcessor.class);
    static final String ID = "id";
    static final String QTY = "qty";
    static final String LEN = "len";
    static final String BEAM = "beam";

    public BeamListTextTableProcessor() {
    }

    @Override
    public boolean isRecognized(String title, String[] columns) {
        boolean titleMatch = ArchiCadBeamListingTextFile.getTitle().equalsIgnoreCase(title.trim().replaceAll("\s+", " "));
        // TODO: A looser match to the title, Elastic? Endgrams?
        if (!titleMatch) {
            log.info("A text file with title: '{}' was checked against Beam Listing template and failed (title mismatch)", title);
            return false;
        }
        List<String> expectedColumns = Arrays.asList(ArchiCadBeamListingTextFile.getColumnArray());
        expectedColumns = expectedColumns.stream().map(m -> m.toLowerCase()).collect(Collectors.toList());
        if (columns.length < expectedColumns.size()) {
            log.info("A text file with title: '{}' was checked against Beam Listing template and failed (column count different)", title);
            return false;
        }
        boolean allColumnsPresent = true;
        for (int i = 0; i < columns.length; i++) {
            log.info("Checking if {} is in : {}", columns[i], Arrays.toString(expectedColumns.toArray()));
            if (!expectedColumns.contains(columns[i].toLowerCase())) {
                allColumnsPresent = false;
                break;
            }
        }
        return allColumnsPresent;
    }

    @Override
    public Map<String, ExtractedDataRow> processData(FileData fileData, String title, String[] columns, List<String[]> dataRows) {
        List<String> cleanColumns = new ArrayList<>();
        for (String column : columns) {
            cleanColumns.add(column.trim().replaceAll("\s+", " "));
        }

        Integer idColumnIdx = detectColumn(cleanColumns, "id");
        Integer lengthColumnIdx = detectColumn(cleanColumns, "len", "length");
        Integer qtyColumnIdx = detectColumn(cleanColumns, "qty", "quantity");
        Integer nameColumnIdx = detectColumn(cleanColumns, "beam", "name", "size");
        if (idColumnIdx == null || lengthColumnIdx == null || qtyColumnIdx == null || nameColumnIdx == null) {
            log.info("A column wasn't found: '{}', '{}', '{}', '{}'", idColumnIdx, lengthColumnIdx, qtyColumnIdx, nameColumnIdx);
        } else {
            log.info("Columns found with indexes: '{}', '{}', '{}', '{}'", idColumnIdx, lengthColumnIdx, qtyColumnIdx, nameColumnIdx);
        }

        Map<String, Integer> columnIndexMap = Map.of(
                ID, idColumnIdx,
                QTY, qtyColumnIdx,
                LEN, lengthColumnIdx,
                BEAM, nameColumnIdx
        );

        Map<String, ExtractedDataRow> processedDataMap = new HashMap<>();
        int count = 1;
        for (String[] rowData : dataRows) {
            Map<String, String> valuesMap = new HashMap<>();
            try {
                processRow(rowData, cleanColumns, valuesMap, columnIndexMap);
                System.out.println("Values map for this row: " + valuesMap.size());
            } catch (InvalidRowException e) {
                log.warn("Encountered an invalid row in the table data: {}", Arrays.toString(rowData));
            }

            var extractedDataRow = ExtractedDataRow.builder()
                    .row(count)
                    .itemId(valuesMap.get(ID))
                    .parent(fileData)
                    .data(valuesMap)
                    .build();
            log.info("Row number='{}', id='{}'", count, valuesMap.get(ID));
            processedDataMap.put(valuesMap.get(ID), extractedDataRow);
            count++;
        }

        return processedDataMap;
    }

    private class InvalidRowException extends Exception {}
    private void processRow(
            String[] dataRow,
            List<String> columns,
            Map<String, String> valuesMap,
            Map<String, Integer> columnIndexMap) throws InvalidRowException {
        if (dataRow.length != columns.size()) {
            throw new InvalidRowException();
        }

        Integer idx_id = columnIndexMap.get(ID);
        Integer idx_qty = columnIndexMap.get(QTY);
        Integer idx_len = columnIndexMap.get(LEN);
        Integer idx_name = columnIndexMap.get(BEAM);

        valuesMap.put(ID, dataRow[idx_id]);
        valuesMap.put(QTY, dataRow[idx_qty]);
        valuesMap.put(LEN, dataRow[idx_len]);
        valuesMap.put(BEAM, dataRow[idx_name]);
    }

    private Integer detectColumn(List<String> columns, String... possibleValues) {
        int idx = 0;
        for (String column : columns) {
            for (String possibleValue : possibleValues) {
                if (column.equalsIgnoreCase(possibleValue)
                        || column.toLowerCase().startsWith(possibleValue.toLowerCase())
                ) {
                    return idx;
                }
            }
            idx++;
        }
        return null;
    }

}
