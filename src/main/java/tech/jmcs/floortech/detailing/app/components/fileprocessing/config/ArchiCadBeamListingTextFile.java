package tech.jmcs.floortech.detailing.app.components.fileprocessing.config;

public class ArchiCadBeamListingTextFile {
    public static String getTitle() {
        return "BEAM SCHEDULE";
    }
    public static String[] getColumnArray() {
        return new String[] {"Beam", "Qty", "ID", "Length"};
    }
}
