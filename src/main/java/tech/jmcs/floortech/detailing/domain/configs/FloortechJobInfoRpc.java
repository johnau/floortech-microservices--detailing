package tech.jmcs.floortech.detailing.domain.configs;

import tech.jmcs.floortech.common.dto.FloortechJobDto;

@Deprecated
public interface FloortechJobInfoRpc {
    FloortechJobDto getJobInformation(String jobId);
}
