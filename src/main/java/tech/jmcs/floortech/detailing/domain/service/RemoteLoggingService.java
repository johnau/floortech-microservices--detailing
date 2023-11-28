package tech.jmcs.floortech.detailing.domain.service;

public interface RemoteLoggingService {
    void sendSystemError(String message);
    void sendSystemInfo(String message);
    void sendBusinessInfo(String message);
}
