package tech.jmcs.floortech.detailing.domain.model.filedata;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Facade/Data accessor - Ensures immutability of domain objects:
 * - Non-primitive types are cloned/deep-copied
 */
public interface ExtractedDataRowFacade {
    Function<ExtractedDataRow, String> toRowId = edr -> edr.id;
    Function<ExtractedDataRow, Integer> toRow = edr -> edr.row;
    Function<ExtractedDataRow, Map<String, String>> toData = edr -> new HashMap<>(edr.data);
    Function<ExtractedDataRow, String> toItemId = edr -> edr.itemId;
    Function<ExtractedDataRow, FileData> toParent = edr -> edr.parent;
}
