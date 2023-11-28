package tech.jmcs.floortech.detailing.domain.model.fileset;

import tech.jmcs.floortech.detailing.domain.configs.XPath;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.domain.model.files.DetailingFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public interface FileSetFacade {
    Function<FileSet, String> toFileSetId = fileSet -> fileSet.id;
    Function<FileSet, DetailingClaim> toDetailingClaim = fileSet -> fileSet.jobClaim;
    Function<FileSet, Date> toCreatedDate = fileSet -> fileSet.createdDate;
    Function<FileSet, XPath> toZipArchivePath = fileSet -> fileSet.zipArchivePath;
    Function<FileSet, String> toLabel = fileSet -> fileSet.label;
    Function<FileSet, List<DetailingFile>> toFiles = fileSet -> new ArrayList<>(fileSet.files);
}
