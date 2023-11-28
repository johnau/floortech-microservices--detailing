package tech.jmcs.floortech.detailing.domain.model.detailingclaim;

import tech.jmcs.floortech.detailing.domain.configs.DetailingStatus;
import tech.jmcs.floortech.detailing.domain.model.fileset.FileSet;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Facade/Data accessor - Ensures immutability of domain objects:
 * - Non-primitive types are cloned/deep-copied
 */
public interface DetailingClaimFacade {
    Function<DetailingClaim, String> toJobId = dc -> dc.jobId;
    Function<DetailingClaim, DetailingStatus> toStatus = dc -> dc.status;
    Function<DetailingClaim, String> toClientName = dc -> dc.jobClientName;
    Function<DetailingClaim, String> toClientId = dc -> dc.jobClientId;
    Function<DetailingClaim, String> toEngineerName = dc -> dc.jobEngineerName;
    Function<DetailingClaim, String> toEngineerId = dc -> dc.jobEngineerId;
    Function<DetailingClaim, Integer> toJobNumber = dc -> dc.floortechJobNumber;
    Function<DetailingClaim, Map<String, FileSet>> toFileSets = dc -> new HashMap<>(dc.fileSets); // TODO: Need to deep copy
    Function<DetailingClaim, String> toClaimedByStaffUsername = dc -> dc.claimedByStaffUsername;
    Function<DetailingClaim, String> toClaimedByStaffUserId = dc -> dc.claimedByStaffId;
    Function<DetailingClaim, Date> toClaimedAt = dc -> dc.claimedAt;
    BiFunction<DetailingClaim, String, FileSet> findFileSet = (dc, id) -> new HashMap<>(dc.fileSets).get(id); // TODO: Need to deep copy
    BiFunction<DetailingClaim, String, Boolean> isOwnedBy = (dc, username) -> {
                                                    Objects.requireNonNull(username);
                                                    return dc.claimedByStaffUsername != null && !dc.claimedByStaffUsername.isBlank() && dc.claimedByStaffUsername.equals(username);
                                                };
    Function<DetailingClaim, Boolean> hasAtLeastOneFileSet = dc -> !dc.fileSets.isEmpty();

    // TODO: This will not be required since the client id received from the remote service will be this.
    @Deprecated(since="Use for tests only")
    Function<DetailingClaim, String> toClientIdentifier = dc -> {
                                                    var jobClientId = dc.jobClientId;
                                                    var jobClientName = dc.jobClientName;
                                                    if (jobClientId == null || jobClientId.isBlank())
                                                        jobClientId = "<Missing>";
                                                    if (jobClientName == null || jobClientName.isBlank())
                                                        jobClientName = "<Missing>";
                                                    return jobClientId + "_" + jobClientName;
                                                };

}
