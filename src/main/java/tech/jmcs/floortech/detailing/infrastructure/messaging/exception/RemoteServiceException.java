package tech.jmcs.floortech.detailing.infrastructure.messaging.exception;

public class RemoteServiceException extends Exception {
    public RemoteServiceException(String message) {
        super(message);
    }

    public static RemoteServiceException noResponse(String serviceName) {
        return new RemoteServiceException("No response from " + serviceName + " service");
    }

    public static RemoteServiceException serviceTimeout(String serviceName) {
        return new RemoteServiceException("No response from " + serviceName + " service (timeout)");
    }
}
