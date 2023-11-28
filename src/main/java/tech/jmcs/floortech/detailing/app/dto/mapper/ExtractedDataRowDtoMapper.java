package tech.jmcs.floortech.detailing.app.dto.mapper;

import org.springframework.stereotype.Component;
import tech.jmcs.floortech.detailing.app.dto.GetExtractedDataRowDto;
import tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRow;
import tech.jmcs.floortech.detailing.domain.model.filedata.FileDataFacade;

import static tech.jmcs.floortech.detailing.domain.model.filedata.ExtractedDataRowFacade.*;

@Component
public class ExtractedDataRowDtoMapper {

    public GetExtractedDataRowDto writeGetDto(ExtractedDataRow extractedDataRow) {
        var id = toRowId.apply(extractedDataRow);
        var row = toRow.apply(extractedDataRow);
        var itemId = toItemId.apply(extractedDataRow);
        var parent = toParent.apply(extractedDataRow);
        var data = toData.apply(extractedDataRow);
        var fileDataId = FileDataFacade.toFileDataId.apply(parent);
        return new GetExtractedDataRowDto(
                id, row, itemId, fileDataId, data
        );
    }

}
