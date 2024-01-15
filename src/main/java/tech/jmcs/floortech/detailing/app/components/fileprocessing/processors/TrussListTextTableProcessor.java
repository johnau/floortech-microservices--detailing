package tech.jmcs.floortech.detailing.app.components.fileprocessing.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.jmcs.floortech.detailing.app.components.fileprocessing.config.ArchiCadTrussListingTextFile;
import tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRow;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;
import tech.jmcs.floortech.detailing.domain.service.fileprocessing.TableProcessor;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class TrussListTextTableProcessor implements TableProcessor {
    static final Logger log = LoggerFactory.getLogger(TrussListTextTableProcessor.class);
    // Values read from table
    static final String ID = "id";
    static final String QTY = "qty";
    static final String LEN = "len";
    static final String TYPE = "type";
    static final String LEC = "LEC"; // Left
    static final String REC = "REC"; // Right
    static final String NEC = "NEC"; // notched endcap
    static final String STC = "SEC"; // standard endcap
    static final String PENO = "peno";
    static final String PENO_POS = "peno position";
    static final String TRUSS_GRP = "truss group";

    public TrussListTextTableProcessor() {
    }

    @Override
    public boolean isRecognized(String title, String[] columns) {
        log.info("Truss List Title={}", title);
        List<String> preparedColumns = new ArrayList<>();
        for (String column : columns) {
            preparedColumns.add(column.trim().replaceAll("\s+", " "));
        }

        boolean titleMatch = false;
        String[] titleParts = title.trim().replaceAll("\s+", " ").split("\s");
        String[] expectedTitleParts = ArchiCadTrussListingTextFile.getTitleParts();
        int matchCount = 0;
        for (String expectedPart : expectedTitleParts) {
            for (String part : titleParts) {
                if (part.toLowerCase().startsWith(expectedPart.toLowerCase())) {
                    matchCount++;
                    break;
                }
            }
        }
        log.info("Title={} ({} matches)\n", title, matchCount);
        if (matchCount == expectedTitleParts.length || matchCount == expectedTitleParts.length - 1) {
            titleMatch = true;
        }

        if (!titleMatch) {
            log.info("A text file (title='{}') was checked against Truss Listing template and failed (title mismatch)");
            return false;
        }

        List<String> expectedColumns = Arrays.asList(ArchiCadTrussListingTextFile.getColumnArray());
        expectedColumns = expectedColumns.stream().map(m -> m.toLowerCase()).collect(Collectors.toList());
        if (columns.length < expectedColumns.size()) {
            log.info("A text file (title='{}') was checked against Truss Listing template and failed (column count different)");
            return false;
        }
        boolean allColumnsPresent = checkColumns(preparedColumns, expectedColumns);
        return allColumnsPresent;
    }

    private boolean checkColumns(List<String> columns, List<String> expectedColumns) {
        int matchingColumnCount = 0;
        for (String expectedColumn : expectedColumns) { // looping over each expected column name
            for (String column : columns) {
                String expectedLwr = expectedColumn.toLowerCase();
                String _columnLwr = column.toLowerCase();
                String columnLwr = _columnLwr;
                if (_columnLwr.length() > 2 && _columnLwr.substring(0, 2).contains(".")) {
                    columnLwr = columnLwr.substring(_columnLwr.indexOf(".") + 1);
                }
                if (columnLwr.contains("(")) {
                    columnLwr = columnLwr.substring(0, columnLwr.indexOf("("));
                }

                if (columnLwr.startsWith(expectedLwr)) {

                    matchingColumnCount++;
                }
            }
        }

        return matchingColumnCount == expectedColumns.size();
    }

    @Override
    public Map<String, ExtractedDataRow> processData(FileData fileData, String title, String[] columns, List<String[]> dataRows) {
        List<String> cleanColumns = new ArrayList<>();
        for (String column : columns) {
            cleanColumns.add(column.trim().replaceAll("\s+", " "));
        }

        System.out.println("Processing data...");

//        "ID", "No", "Truss Length", "Type", "Left End Cap", "Right End Cap", "NEC", "STD", "Has Peno", "Cut Webs", "Truss Grouping Pack"
        Integer idxIdColumn = detectColumn(cleanColumns, "id");
        Integer idxQtyColumn = detectColumn(cleanColumns, "no", "qty");
        Integer idxLengthColumn = detectColumn(cleanColumns, "truss length");
        Integer idxTypeColumn = detectColumn(cleanColumns, "type");
        Integer idxLeftEcColumn = detectColumn(cleanColumns, "left end cap");
        Integer idxRightEcColumn = detectColumn(cleanColumns, "right end cap");
        Integer idxNecColumn = detectColumn(cleanColumns, "nec");
        Integer idxStdColumn = detectColumn(cleanColumns, "std");
        Integer idxHasPenoColumn = detectColumn(cleanColumns, "has peno", "p.has peno");
        Integer idxCutWebsColumn = detectColumn(cleanColumns, "cut webs", "p.cut webs");
        Integer idxTrussGroupColumn = detectColumn(cleanColumns, "truss grouping pack", "truss group");
        if (idxIdColumn == null || idxQtyColumn == null || idxLengthColumn == null || idxTypeColumn == null
                || idxLeftEcColumn == null || idxRightEcColumn == null || idxNecColumn == null || idxStdColumn == null
                || idxHasPenoColumn == null || idxCutWebsColumn == null || idxTrussGroupColumn == null) {
            System.out.printf("A column wasn't found: %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s \n",
                    idxIdColumn, idxQtyColumn, idxLengthColumn, idxTypeColumn, idxLeftEcColumn, idxRightEcColumn,
                    idxNecColumn, idxStdColumn, idxHasPenoColumn, idxCutWebsColumn, idxTrussGroupColumn);
        }
        Map<String, Integer> columnIndexMap = new HashMap<>();
        columnIndexMap.put(ID, idxIdColumn);
        columnIndexMap.put(QTY, idxQtyColumn);
        columnIndexMap.put(LEN, idxLengthColumn);
        columnIndexMap.put(TYPE, idxTypeColumn);
        columnIndexMap.put(LEC, idxLeftEcColumn);
        columnIndexMap.put(REC, idxRightEcColumn);
        columnIndexMap.put(NEC, idxNecColumn);
        columnIndexMap.put(STC, idxStdColumn);
        columnIndexMap.put(PENO, idxHasPenoColumn);
        columnIndexMap.put(PENO_POS, idxCutWebsColumn);
        columnIndexMap.put(TRUSS_GRP, idxTrussGroupColumn);

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
            System.out.println("Row number: " + count + ", with id: " + valuesMap.get(ID));

            processedDataMap.put(valuesMap.get(ID), extractedDataRow);
            count++;
        }

        return processedDataMap;
    }

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
        Integer idx_type = columnIndexMap.get(TYPE);
        Integer idx_lec = columnIndexMap.get(LEC);
        Integer idx_rec = columnIndexMap.get(REC);
        Integer idx_nec = columnIndexMap.get(NEC);
        Integer idx_stc = columnIndexMap.get(STC);
        Integer idx_peno = columnIndexMap.get(PENO);
        Integer idx_peno_pos = columnIndexMap.get(PENO_POS);
        Integer idx_truss_grp = columnIndexMap.get(TRUSS_GRP);

        valuesMap.put(ID, dataRow[idx_id]);
        valuesMap.put(QTY, dataRow[idx_qty]);
        valuesMap.put(LEN, dataRow[idx_len]);
        valuesMap.put(TYPE, dataRow[idx_type]);
        valuesMap.put(LEC, dataRow[idx_lec]);
        valuesMap.put(REC, dataRow[idx_rec]);
        valuesMap.put(NEC, dataRow[idx_nec]);
        valuesMap.put(STC, dataRow[idx_stc]);
        valuesMap.put(PENO, dataRow[idx_peno]);
        valuesMap.put(PENO_POS, dataRow[idx_peno_pos]);
        valuesMap.put(TRUSS_GRP, dataRow[idx_truss_grp]);
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

    private class InvalidRowException extends Exception {
    }
}
