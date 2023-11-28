package tech.jmcs.floortech.detailing.app.dto.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.jmcs.floortech.detailing.app.dto.GetDetailingFileDto;
import tech.jmcs.floortech.detailing.app.dto.GetFileDataDto;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;

import static tech.jmcs.floortech.detailing.domain.model.files.DetailingFileFacade.*;

@Component
public class DetailingFileDtoMapper {

    private final FileDataDtoMapper fileDataDtoMapper;

    @Autowired
    public DetailingFileDtoMapper(FileDataDtoMapper fileDataDtoMapper) {
        this.fileDataDtoMapper = fileDataDtoMapper;
    }

    public GetDetailingFileDto toGetDto(DetailingFile detailingFile) {
        var id = toFileId.apply(detailingFile);
        var path = toPath.apply(detailingFile);
        var extension = toExtension.apply(detailingFile);
        var filename = toFilename.apply(detailingFile);
        var fileSize = toFileSize.apply(detailingFile);
        var mimetype = toMimeType.apply(detailingFile);
        var parentFolder = toParentFolder.apply(detailingFile);
        var label = toLabel.apply(detailingFile);
        var creationDate = toCreationDate.apply(detailingFile);
        var fileData = toFileData.apply(detailingFile);

        GetFileDataDto getFileDataDto = null;
        if (fileData != null) {
            getFileDataDto = fileDataDtoMapper.writeGetDto(fileData);
        }

        return new GetDetailingFileDto(
                id, path, extension, filename, fileSize, mimetype, parentFolder, label, creationDate,
                getFileDataDto
        );
    }
}
