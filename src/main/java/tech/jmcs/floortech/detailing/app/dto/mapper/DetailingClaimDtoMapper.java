package tech.jmcs.floortech.detailing.app.dto.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.jmcs.floortech.detailing.app.dto.GetDetailingClaimDto;
import tech.jmcs.floortech.detailing.app.dto.GetFileSetDto;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaimFacade;

import java.util.Map;
import java.util.stream.Collectors;

import static tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaimFacade.*;

@Component
public class DetailingClaimDtoMapper {

    private final FileSetDtoMapper fileSetDtoMapper;

    @Autowired
    public DetailingClaimDtoMapper(FileSetDtoMapper fileSetDtoMapper) {
        this.fileSetDtoMapper = fileSetDtoMapper;
    }

    public GetDetailingClaimDto toGetDto(DetailingClaim detailingClaim) {
        var jobId = toJobId.apply(detailingClaim);
        var floortechJobNumber = toJobNumber.apply(detailingClaim);
        var jobClientId = toClientId.apply(detailingClaim);
        var jobClientName = toClientName.apply(detailingClaim);
        var jobEngineerId = toEngineerId.apply(detailingClaim);
        var jobEngineerName = toEngineerName.apply(detailingClaim);
        var claimedByStaffUsername = toClaimedByStaffUsername.apply(detailingClaim);
        var claimedByStaffId = toClaimedByStaffUserId.apply(detailingClaim);
        var claimedAt = toClaimedAt.apply(detailingClaim);
        var fileSets = toFileSets.apply(detailingClaim);
        var status = toStatus.apply(detailingClaim);

        Map<String, GetFileSetDto> fileSetDtos = Map.of();
        if (fileSets != null) {
            fileSetDtos = fileSets.entrySet().stream()
                    .map(entry -> {
                        var key = entry.getKey();
                        var value = fileSetDtoMapper.toGetDto(entry.getValue());
                        return Map.entry(key, value);
                    })
                    .collect(Collectors.toMap(
                            e -> e.getKey(),
                            e -> e.getValue()
                    ));
        }
        return new GetDetailingClaimDto(
                null, jobId, floortechJobNumber, jobClientId, jobClientName, jobEngineerId, jobEngineerName,
                claimedByStaffUsername, claimedByStaffId,
                claimedAt, fileSetDtos, status
        );
    }

}
