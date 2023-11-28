package tech.jmcs.floortech.detailing.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tech.jmcs.floortech.detailing.infrastructure.persistence.dao.DetailingClaimDao;

@WebFluxTest(DetailingClaimRepositoryImpl.class)
@ExtendWith(SpringExtension.class)
public class DetailingClaimRepositoryImplTests {

    @Autowired
    private DetailingClaimRepositoryImpl detailingClaimRepository;

    @MockBean
    private DetailingClaimDao detailingClaimDao;

    @Test
    public void shouldCreateClaim() {
//        var dto = new CreateDetailingClaimDto("JOB-0001", "bob", "STAFF-0001", new Date());
//        var domainObject = dto.toDomainObject();
//
//        var entityWithId = DetailingClaimEntity.builder()
//                .id("XXXXXXXX")
//                .jobId(DetailingClaimFacade.toJobId.apply(domainObject))
//                .claimedByStaffId(DetailingClaimFacade.toClaimedByStaffUserId.apply(domainObject))
//                .claimedByStaffUsername(DetailingClaimFacade.toClaimedByStaffUsername.apply(domainObject))
//                .createdDate(new Date())
//                .status(DetailingStatus.UNVERIFIED)
//                .build();
//
//        Mockito.when(detailingClaimDao.save(any(DetailingClaimEntity.class))).thenReturn(Mono.just(entityWithId));
//        var saveMono = detailingClaimRepository.save(domainObject);
//        StepVerifier.create(saveMono)
//                .consumeNextWith(saved -> {
//                    System.out.println(saved);
//                    assertEquals(true, DetailingClaimFacade.toJobId.apply(saved).equals("JOB-0001"));
//                })
//                .verifyComplete();
    }

}
