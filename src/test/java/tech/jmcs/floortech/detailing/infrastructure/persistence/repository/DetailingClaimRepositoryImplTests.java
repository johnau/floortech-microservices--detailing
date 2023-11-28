package tech.jmcs.floortech.detailing.infrastructure.persistence.repository;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@WebFluxTest(DetailingClaimRepositoryImpl.class)
@ExtendWith(SpringExtension.class)
public class DetailingClaimRepositoryImplTests {
}
