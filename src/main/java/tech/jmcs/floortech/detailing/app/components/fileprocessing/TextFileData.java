package tech.jmcs.floortech.detailing.app.components.fileprocessing;

import java.util.List;
import java.util.Map;

public record TextFileData(
    String title,
    String[] columns,
    List<String[]> dataRows,
    Map<Integer, String> rawDataMap){
}