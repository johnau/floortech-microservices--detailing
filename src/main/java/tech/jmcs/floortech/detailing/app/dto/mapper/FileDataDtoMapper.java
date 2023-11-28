package tech.jmcs.floortech.detailing.app.dto.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.jmcs.floortech.detailing.app.dto.GetFileDataDto;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileData;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFileFacade;

import java.util.Map;
import java.util.stream.Collectors;

import static tech.jmcs.floortech.detailing.domain.model.filedata.FileDataFacade.*;

@Component
public class FileDataDtoMapper {

    private final ExtractedDataRowDtoMapper extractedDataRowDtoMapper;

    @Autowired
    public FileDataDtoMapper(ExtractedDataRowDtoMapper extractedDataRowDtoMapper) {
        this.extractedDataRowDtoMapper = extractedDataRowDtoMapper;
    }

    public GetFileDataDto writeGetDto(FileData fileData) {
        var id = toFileDataId.apply(fileData);
        var lines = toLines.apply(fileData);
        var extractedData = toExtractedData.apply(fileData);
        var parent = toDetailingFile.apply(fileData);

        var extractedDataDtos = extractedData.entrySet().stream()
                .map(entrySet -> {
                    var key = entrySet.getKey();
                    var dto = extractedDataRowDtoMapper.writeGetDto(entrySet.getValue());
                    return Map.entry(key, dto);
                })
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue()
                ));
        var parentFileId = DetailingFileFacade.toFileId.apply(parent);
        return new GetFileDataDto(
                id, lines, extractedDataDtos, parentFileId
        );
    }
}
