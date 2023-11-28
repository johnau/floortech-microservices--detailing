package tech.jmcs.floortech.detailing.domain.service;

import reactor.core.publisher.Mono;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;

public interface FloortechJobDataService {
    Mono<DetailingClaim> requestDataAndUpdate(DetailingClaim detailingClaim);
}
