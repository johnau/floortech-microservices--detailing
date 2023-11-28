package tech.jmcs.floortech.detailing.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.jmcs.floortech.common.dto.FloortechJobDto;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.domain.service.DtoToEntityMapper;
import tech.jmcs.floortech.detailing.domain.service.FloortechJobDataService;
import tech.jmcs.floortech.detailing.infrastructure.messaging.exception.RemoteServiceException;
import tech.jmcs.floortech.detailing.infrastructure.messaging.helper.FloortechJobDtoConverter;

import java.time.Duration;

import static tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaimFacade.toJobId;

@Component
public class FloortechJobDataServiceImpl implements FloortechJobDataService {
    final static Logger log = LoggerFactory.getLogger(FloortechJobDataServiceImpl.class);
    final RpcMessageSender rpcMessageSender;
    final FloortechJobDtoConverter floortechJobDtoConverter;
    final DtoToEntityMapper<FloortechJobDto, DetailingClaim> floortechJobDtoToDetailingClaimMapper;

    @Autowired
    public FloortechJobDataServiceImpl(RpcMessageSender rpcMessageSender, FloortechJobDtoConverter floortechJobDtoConverter, DtoToEntityMapper<FloortechJobDto, DetailingClaim> floortechJobDtoToDetailingClaimMapper) {
        this.rpcMessageSender = rpcMessageSender;
        this.floortechJobDtoConverter = floortechJobDtoConverter;
        this.floortechJobDtoToDetailingClaimMapper = floortechJobDtoToDetailingClaimMapper;
    }

    @Override
    public Mono<DetailingClaim> requestDataAndUpdate(DetailingClaim detailingClaimWithoutJobData) {
        var jobId = toJobId.apply(detailingClaimWithoutJobData);
        return rpcMessageSender.sendJobInfoRpc(jobId, null)
                .onErrorMap(error -> RemoteServiceException.noResponse("Floortech Job Info"))
                .timeout(Duration.ofMillis(5000), Mono.error(RemoteServiceException.serviceTimeout("Floortech Job Info")))
                .map(delivery -> floortechJobDtoConverter.convert(delivery.getBody()))
                .doOnNext(floortechJobDto -> log.info("Job information response received: (Job number: {}, Address: {})", floortechJobDto.getJobNumber(), floortechJobDto.getAddress().toString()))
                .map(floortechJobDtoToDetailingClaimMapper::map)
                .map(detailingClaimWithoutJobData::updateIgnoringNullsAndEmpty);
    }

}
