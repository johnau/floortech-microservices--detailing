package tech.jmcs.floortech.detailing.app.dto;

import java.util.Map;

public record GetExtractedDataRowDto(
    String id,
    Integer row,
    String itemId,
    String parentId,
    Map<String, String> data
) {
}
