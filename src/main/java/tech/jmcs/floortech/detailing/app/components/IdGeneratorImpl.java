package tech.jmcs.floortech.detailing.app.components;

import org.springframework.stereotype.Service;
import tech.jmcs.floortech.detailing.domain.service.IdGenerator;

import java.util.UUID;

@Service
public class IdGeneratorImpl implements IdGenerator {
    public String generateUuid() {
        return UUID.randomUUID().toString();
    }
}
