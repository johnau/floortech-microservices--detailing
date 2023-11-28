package tech.jmcs.floortech.detailing.app.dto.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.jmcs.floortech.detailing.app.dto.GetDetailingFileDto;
import tech.jmcs.floortech.detailing.app.dto.GetFileSetDto;
import tech.jmcs.floortech.detailing.domain.model.fileset.FileSet;

import java.util.List;
import java.util.stream.Collectors;

import static tech.jmcs.floortech.detailing.domain.model.fileset.FileSetFacade.*;

@Component
public class FileSetDtoMapper {

    private final DetailingFileDtoMapper detailingFileDtoMapper;

    @Autowired
    public FileSetDtoMapper(DetailingFileDtoMapper detailingFileDtoMapper) {
        this.detailingFileDtoMapper = detailingFileDtoMapper;
    }

    public GetFileSetDto toGetDto(FileSet fileSet) {
        var id = toFileSetId.apply(fileSet);
        var label = toLabel.apply(fileSet);
        var files = toFiles.apply(fileSet);
        var jobClaim = toDetailingClaim.apply(fileSet);
        var zipArchivePath = toZipArchivePath.apply(fileSet);
        var createdDate = toCreatedDate.apply(fileSet);

        List<GetDetailingFileDto> fileDtos = List.of();
        if (files != null) {
            fileDtos = files.stream()
                    .map(detailingFileDtoMapper::toGetDto)
                    .collect(Collectors.toList());
        }

        return new GetFileSetDto(
                id, label, fileDtos, null, zipArchivePath, createdDate
        );
    }
}
