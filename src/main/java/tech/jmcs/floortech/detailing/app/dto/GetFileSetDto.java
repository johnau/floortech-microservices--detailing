package tech.jmcs.floortech.detailing.app.dto;

import tech.jmcs.floortech.detailing.domain.configs.XPath;

import java.util.Date;
import java.util.List;

public record GetFileSetDto(
        String id,
        String label,
        List<GetDetailingFileDto> files,
        String jobClaimId,
        XPath zipArchiveXPath,
        Date createdDate
) {
}
