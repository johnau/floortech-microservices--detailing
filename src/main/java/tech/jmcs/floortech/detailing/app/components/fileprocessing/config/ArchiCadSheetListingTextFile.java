package tech.jmcs.floortech.detailing.app.components.fileprocessing.config;

public class ArchiCadSheetListingTextFile {
    public static String getTitle() {
        return "SHEETS";
    }
    public static String[] getColumnArray() {
        return new String[] {"ID", "Length", "Qty"};
    }
}
