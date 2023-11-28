package tech.jmcs.floortech.detailing.infrastructure.messaging.helper;

import org.springframework.stereotype.Component;
import tech.jmcs.floortech.common.dto.FloortechJobDto;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.domain.service.DtoToEntityMapper;

import java.util.Objects;

@Component
public class FloortechJobDtoToDetailingClaimMapper implements DtoToEntityMapper<FloortechJobDto, DetailingClaim> {
    public DetailingClaim map(FloortechJobDto dto) {
        Objects.requireNonNull(dto);
        var detailingClaim = DetailingClaim.builder("", "").build();
        var jobNumber = dto.getJobNumber();
        var clientId = dto.getClient().getUuid();
        var clientName = dto.getClient().getCompanyName();
        var engineerId = dto.getEngineer().getUuid();
        var engineerName = dto.getEngineer().getCompanyName();
        return detailingClaim.withFloortechJobNumber(jobNumber)
                .withClientId(clientId)
                .withClientName(clientName)
                .withEngineerId(engineerId)
                .withEngineerName(engineerName);
    }
}
