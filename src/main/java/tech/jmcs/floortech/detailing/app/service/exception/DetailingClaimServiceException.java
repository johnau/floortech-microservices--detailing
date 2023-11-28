package tech.jmcs.floortech.detailing.app.service.exception;

public class DetailingClaimServiceException extends Exception {

    public DetailingClaimServiceException(String message) {
        super(message);
    }

    public static DetailingClaimServiceException jobAlreadyClaimed(String owner) {
        return new DetailingClaimServiceException("Already claimed by " + owner);
    }
    public static DetailingClaimServiceException badJobIdFormat() {
        return new DetailingClaimServiceException("Job ID formatted incorrectly");
    }
    public static DetailingClaimServiceException claimIdDoesNotExist(String claimId) {
        return new DetailingClaimServiceException("Could not find Claim with ID: " + claimId);
    }
    public static DetailingClaimServiceException claimForJobIdDoesNotExist(String jobId) {
        return new DetailingClaimServiceException("Could not find Claim with Job ID: " + jobId);
    }
    public static DetailingClaimServiceException notClaimOwner() {
        return new DetailingClaimServiceException("Current user does not own this claim");
    }
    public static DetailingClaimServiceException cantCompleteNoFileSet() {
        return new DetailingClaimServiceException("This claim does not have a file set and therefor cannot be completed");
    }
    public static DetailingClaimServiceException tooManyRequests(Integer requestLimit) {
        return new DetailingClaimServiceException("Can not request more than " + requestLimit + " items");
    }
    public static DetailingClaimServiceException noResponse(String serviceName) {
        return new DetailingClaimServiceException("No response from " + serviceName + " service");
    }
    public static DetailingClaimServiceException serviceTimeout(String serviceName) {
        return new DetailingClaimServiceException("No response from " + serviceName + " service (timeout)");
    }
    public static DetailingClaimServiceException cannotCompleteClaim() {
        return new DetailingClaimServiceException("Unable to complete this Detailing Claim.  Ensure you are the owner and have provided a Detailing File Set.");
    }
    public static DetailingClaimServiceException claimNotFound() {
        return new DetailingClaimServiceException("Claim not found!");
    }

    public static DetailingClaimServiceException claimNotFound(String jobId, String currentUser) {
        return new DetailingClaimServiceException("Could not find Claim for Id: " + jobId + "-" + currentUser);
    }
}
