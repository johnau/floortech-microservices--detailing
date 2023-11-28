package tech.jmcs.floortech.detailing.infrastructure.messaging.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import tech.jmcs.floortech.common.dto.FloortechJobDto;

import java.io.IOException;

@Component
public class FloortechJobDtoConverter implements Converter<byte[], FloortechJobDto> {
    static final Logger log = LoggerFactory.getLogger(FloortechJobDtoConverter.class);
    @Override
    public FloortechJobDto convert(byte[] json) {
        var mapper = new ObjectMapper();
        FloortechJobDto dto;
        try {
            dto = mapper.readValue(json, FloortechJobDto.class);
        } catch (IOException e) {
            log.error("Could not convert JSON to Java Object: common.dto.FloortechJobDtoConverter");
            return null;
        }
        return dto;
    }
}
