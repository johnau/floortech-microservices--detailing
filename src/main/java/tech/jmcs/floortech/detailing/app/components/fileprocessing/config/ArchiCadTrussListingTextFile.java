package tech.jmcs.floortech.detailing.app.components.fileprocessing.config;

public class ArchiCadTrussListingTextFile {
    public static String[] getTitleParts() {
        return new String[]{"CW", "JOIST", "SCHEDULE"};
    }
    public static String[] getColumnArray() {
        return new String[] {"ID", "No", "Truss Length", "Type", "Left End Cap", "Right End Cap", "NEC", "STD", "Has Peno", "Cut Webs", "Truss Grouping Pack"};
    }
}
