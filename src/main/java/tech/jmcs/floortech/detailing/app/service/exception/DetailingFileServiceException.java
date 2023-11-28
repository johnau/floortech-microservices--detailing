package tech.jmcs.floortech.detailing.app.service.exception;

public class DetailingFileServiceException extends Exception {
    public DetailingFileServiceException(String message) {
        super(message);
    }

    public static DetailingFileServiceException detailingFilePathIsNull() {
        return new DetailingFileServiceException("DetailingFile.path was null.  There cannot be a DetailingFile without a path");
    }
    public static DetailingFileServiceException noClaimForId(String claimId) {
        return new DetailingFileServiceException("No claim exists for id: " + claimId);
    }
    public static DetailingFileServiceException noClaimFound(String jobId, String username) {
        return new DetailingFileServiceException("No claim exists for id: " + jobId + " - " + username);
    }
    public static DetailingFileServiceException notClaimOwner() {
        return new DetailingFileServiceException("Not the current owner of this claim.");
    }
    public static DetailingFileServiceException noFileSetForId(String fileSetId) {
        return new DetailingFileServiceException("Could not find FileSet with id: " + fileSetId);
    }
    public static DetailingFileServiceException contentLengthError(long contentLength) {
        var contentInMb = contentLength/1000/1000;
        return new DetailingFileServiceException("Empty files and files larger than "+contentInMb+"MB are not accepted.");
    }
}
