package tech.jmcs.floortech.detailing.domain.model.filedata;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Accessor Facade for DetailingClaim, should not be used by Service layer (except *possibly* for id)
 * ID logic changing to composite ID and will use a different method in the DetailingClaim object
 *
 * Facade/ accessor - Responsible for ensuring the immutability of the underlying class DetailingClaim
 * Non-primitive types are cloned/deep-copied
 */
public interface ExtractedDataRowFacade {
    Function<ExtractedDataRow, String> toRowId = edr -> edr.id;
    Function<ExtractedDataRow, Integer> toRow = edr -> edr.row;
    Function<ExtractedDataRow, Map<String, String>> toData = edr -> new HashMap<>(edr.data);
    Function<ExtractedDataRow, String> toItemId = edr -> edr.itemId;
    Function<ExtractedDataRow, FileData> toParent = edr -> edr.parent;
}
