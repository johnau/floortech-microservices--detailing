package tech.jmcs.floortech.detailing.app.dto;

import java.util.Map;

public record GetFileDataDto(
        String id,
        Map<Integer, String> lines,
        Map<String, GetExtractedDataRowDto> extractedData,
        String fileId
) {
}
