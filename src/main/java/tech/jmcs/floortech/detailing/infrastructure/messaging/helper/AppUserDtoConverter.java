package tech.jmcs.floortech.detailing.infrastructure.messaging.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import tech.jmcs.floortech.common.dto.AppUserDto;

import java.io.IOException;

@Component
public class AppUserDtoConverter implements Converter<byte[], AppUserDto> {
    static final Logger log = LoggerFactory.getLogger(AppUserDtoConverter.class);
    @Override
    public AppUserDto convert(byte[] json) {
        if (json == null) return null;
        var mapper = new ObjectMapper();
        AppUserDto dto;
        try {
            dto = mapper.readValue(json, AppUserDto.class);
        } catch (IOException e) {
            log.error("Could not convert JSON to Java Object: common.dto.AppUserDto");
            return null;
        }
        return dto;
    }
}
