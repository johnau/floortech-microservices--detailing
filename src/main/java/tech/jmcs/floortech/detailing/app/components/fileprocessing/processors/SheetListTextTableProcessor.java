package tech.jmcs.floortech.detailing.app.components.fileprocessing.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.jmcs.floortech.detailing.app.components.fileprocessing.config.ArchiCadSheetListingTextFile;
import tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRow;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;
import tech.jmcs.floortech.detailing.domain.service.fileprocessing.TableProcessor;

import java.util.*;
import java.util.stream.Collectors;

public class SheetListTextTableProcessor implements TableProcessor {
    final static Logger log = LoggerFactory.getLogger(SheetListTextTableProcessor.class);
    static final String ID = "id";
    static final String QTY = "qty";
    static final String LEN = "len";
    final static Logger logger = LoggerFactory.getLogger(SheetListTextTableProcessor.class);

    @Override
    public boolean isRecognized(String title, String[] columns) {
        System.out.println("Checking title in Truss List Procesor: " + title);

        boolean titleMatch = ArchiCadSheetListingTextFile.getTitle().equalsIgnoreCase(title.trim().replaceAll("\s+", " "));
        // TODO: A looser match to the title, Elastic? Endgrams?
        if (!titleMatch) {
            logger.info("A text file with title: '{}' was checked against Sheet Listing template and failed (title mismatch)", title);
            return false;
        }

        List<String> preparedColumns = new ArrayList<>();
        for (String column : columns) {
            preparedColumns.add(column.trim().replaceAll("\s+", " "));
        }
        List<String> expectedColumns = Arrays.asList(ArchiCadSheetListingTextFile.getColumnArray());
        expectedColumns = expectedColumns.stream().map(m -> m.toLowerCase()).collect(Collectors.toList());
        if (preparedColumns.size() < expectedColumns.size()) {
            logger.info("A text file with title: '{}' was checked against Sheet Listing template and failed (column count different)", title);
            return false;
        }
        boolean allColumnsPresent = true;
        for (int i = 0; i < columns.length; i++) {
            logger.info("Checking if {} is in : {}", preparedColumns.get(i), Arrays.toString(expectedColumns.toArray()));
            if (!expectedColumns.contains(preparedColumns.get(i).toLowerCase())) {
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

        System.out.println("Processing data...");

        Integer idColumnIdx = detectColumn(cleanColumns, "id");
        Integer lengthColumnIdx = detectColumn(cleanColumns, "len", "length");
        Integer qtyColumnIdx = detectColumn(cleanColumns, "qty", "quantity");
        if (idColumnIdx == null || lengthColumnIdx == null || qtyColumnIdx == null ) {
            System.out.printf("A column wasn't found: %s, %s, %s \n", idColumnIdx, lengthColumnIdx, qtyColumnIdx);
        } else {
            System.out.printf("Columns found with indexes: %s, %s, %s \n", idColumnIdx, lengthColumnIdx, qtyColumnIdx);
        }

        Map<String, Integer> columnIndexMap = Map.of(
                ID, idColumnIdx,
                QTY, qtyColumnIdx,
                LEN, lengthColumnIdx
        );

        Map<String, ExtractedDataRow> processedDataMap = new HashMap<>();

        int count = 1;
        for (String[] rowData : dataRows) {
            Map<String, String> valuesMap = new HashMap<>();
            try {
                processRow(rowData, cleanColumns, valuesMap, columnIndexMap);
                System.out.println("Values map for this row: " + valuesMap.size());
            } catch (InvalidRowException e) {
                logger.warn("Encountered an invalid row in the table data: {}", Arrays.toString(rowData));
            }

            var extractedDataRow = ExtractedDataRow.builder()
                    .row(count)
                    .itemId(valuesMap.get(ID))
                    .parent(fileData)
                    .data(valuesMap)
                    .build();
            System.out.println("Row number: " + count + ", with id: " + valuesMap.get(ID));

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

        valuesMap.put(ID, dataRow[idx_id]);
        valuesMap.put(QTY, dataRow[idx_qty]);
        valuesMap.put(LEN, dataRow[idx_len]);
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
