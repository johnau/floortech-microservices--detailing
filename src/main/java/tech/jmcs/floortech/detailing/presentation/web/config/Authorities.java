package tech.jmcs.floortech.detailing.presentation.web.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static tech.jmcs.floortech.detailing.presentation.web.config.Authorities.ServiceAuthorities.*;

public class Authorities {

    public enum ServiceAuthorities {
        CREATE_JOB,
        READ_JOB,
        UPDATE_JOB,
        DELETE_JOB,
        CREATE_CLIENT,
        READ_CLIENT,
        UPDATE_CLIENT,
        DELETE_CLIENT,
        CREATE_ENGINEER,
        READ_ENGINEER,
        UPDATE_ENGINEER,
        DELETE_ENGINEER,
    }

    public static List<String> getAuthoritiesForRole(String role) {
        switch(role) {
            case "ROLE_ADMIN":
                return adminAuthorities;
            case "ROLE_FT_STAFF":
                return ftStaffAuthorities;
            case "ROLE_CLIENT":
                return clientAuthorties;
            default:
                return List.of();
        }
    }

    public static final List<String> adminAuthorities = Arrays.stream(ServiceAuthorities.values()).map(a -> a.toString()).collect(Collectors.toList());
    public static final List<String> ftStaffAuthorities = List.of(
            CREATE_JOB.toString(),
            READ_JOB.toString(),
            UPDATE_JOB.toString(),
            CREATE_CLIENT.toString(),
            READ_CLIENT.toString(),
            UPDATE_CLIENT.toString(),
            READ_ENGINEER.toString(),
            UPDATE_ENGINEER.toString());
    public static final List<String> clientAuthorties = List.of(
            CREATE_JOB.toString(),
            READ_JOB.toString(),
            UPDATE_JOB.toString());

}

