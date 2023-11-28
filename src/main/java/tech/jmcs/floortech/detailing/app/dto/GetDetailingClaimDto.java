package tech.jmcs.floortech.detailing.app.dto;

import tech.jmcs.floortech.detailing.domain.configs.DetailingStatus;

import java.util.Date;
import java.util.Map;

public record GetDetailingClaimDto(
        String id,
        String jobId,
        Integer floortechJobNumber,
        String jobClientId,
        String jobClientName,
        String jobEngineerId,
        String jobEngineerName,
        String claimedByStaffUsername,
        String claimedByStaffId,
        Date claimedAt,
        Map<String, GetFileSetDto> fileSets,
        DetailingStatus status
) {
}
