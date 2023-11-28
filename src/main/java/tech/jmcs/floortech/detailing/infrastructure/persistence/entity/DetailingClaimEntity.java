package tech.jmcs.floortech.detailing.infrastructure.persistence.entity;

import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import tech.jmcs.floortech.detailing.domain.configs.DetailingStatus;
import tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaim;
import tech.jmcs.floortech.detailing.domain.model.fileset.FileSet;

import java.util.*;
import java.util.stream.Collectors;

import static tech.jmcs.floortech.detailing.domain.model.detailingclaim.DetailingClaimFacade.*;

@Document(collection = "detailing_claim")
@CompoundIndexes({
        @CompoundIndex(name = "compound_uid", def = "{'jobId' : -1, 'claimedByStaffUsername' : 1, 'createdDate': -1}", unique = true)
})
public class DetailingClaimEntity extends AuditBase {
    @Id
    String id;
    @Indexed
    String jobId;
    @Indexed
    Integer floortechJobNumber;
    @Indexed
    String jobClientId;
    @Indexed
    String jobClientName;
    String jobEngineerId;
    String jobEngineerName;
    @Indexed
    String claimedByStaffUsername;
    @Indexed
    String claimedByStaffId;
//    @DocumentReference(lazy = false, lookup = "{ 'primaryAddress' : ?#{#self._id} }")
    Map<String, FileSetEntity> fileSets;
    @Indexed
    DetailingStatus status;

    public static DetailingClaimEntity fromDomainObject(DetailingClaim detailingClaim) {
        var jobId = toJobId.apply(detailingClaim);
        var floortechJobNumber = toJobNumber.apply(detailingClaim);
        var jobClientId = toClientId.apply(detailingClaim);
        var jobClientName = toClientName.apply(detailingClaim);
        var jobEngineerId = toEngineerId.apply(detailingClaim);
        var jobEngineerName = toEngineerName.apply(detailingClaim);
        var claimedByStaffUsername = toClaimedByStaffUsername.apply(detailingClaim);
        var claimedByStaffId = toClaimedByStaffUserId.apply(detailingClaim);
        var fileSets = toFileSets.apply(detailingClaim);
        Map<String, FileSetEntity> fileSetEntities = new HashMap<>();
        if (fileSets != null) {
            fileSetEntities = toFileSets.apply(detailingClaim).entrySet().stream()
                    .map(entry -> {
                        var key = entry.getKey();
                        var value = FileSetEntity.fromDomainObject(entry.getValue());
                        return Map.entry(key, value);
                    })
                    .collect(Collectors.toMap(
                            e -> (String) e.getKey(),
                            e -> (FileSetEntity) e.getValue()
                    ));
        }
        var status = toStatus.apply(detailingClaim);

        var d = new DetailingClaimEntity();
        d.setJobId(jobId);
        d.setFloortechJobNumber(floortechJobNumber);
        d.setJobClientId(jobClientId);
        d.setJobClientName(jobClientName);
        d.setJobEngineerId(jobEngineerId);
        d.setJobEngineerName(jobEngineerName);
        d.setClaimedByStaffUsername(claimedByStaffUsername);
        d.setClaimedByStaffId(claimedByStaffId);
        d.setFileSets(fileSetEntities);
        d.setStatus(status);
        return d;
    }

    public DetailingClaimEntity() {
    }

    // <editor-fold desc="Getters">
    public String getId() {
        return id;
    }

    public String getJobId() {
        return jobId;
    }

    public Integer getFloortechJobNumber() {
        return floortechJobNumber;
    }

    public String getJobClientId() {
        return jobClientId;
    }

    public String getJobClientName() {
        return jobClientName;
    }

    public String getJobEngineerId() {
        return jobEngineerId;
    }

    public String getJobEngineerName() {
        return jobEngineerName;
    }

    public String getClaimedByStaffUsername() {
        return claimedByStaffUsername;
    }

    public String getClaimedByStaffId() {
        return claimedByStaffId;
    }

    public Map<String, FileSetEntity> getFileSets() {
        return fileSets;
    }

    public DetailingStatus getStatus() {
        return status;
    }
    //</editor-fold>

    // <editor-fold desc="Setters">
    public void setId(String id) {
        this.id = id;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setFloortechJobNumber(Integer floortechJobNumber) {
        this.floortechJobNumber = floortechJobNumber;
    }

    public void setJobClientId(String jobClientId) {
        this.jobClientId = jobClientId;
    }

    public void setJobClientName(String jobClientName) {
        this.jobClientName = jobClientName;
    }

    public void setJobEngineerId(String jobEngineerId) {
        this.jobEngineerId = jobEngineerId;
    }

    public void setJobEngineerName(String jobEngineerName) {
        this.jobEngineerName = jobEngineerName;
    }

    public void setClaimedByStaffUsername(String claimedByStaffUsername) {
        this.claimedByStaffUsername = claimedByStaffUsername;
    }

    public void setClaimedByStaffId(String claimedByStaffId) {
        this.claimedByStaffId = claimedByStaffId;
    }

    public void setFileSets(Map<String, FileSetEntity> fileSets) {
        this.fileSets = fileSets;
    }

    public void setStatus(DetailingStatus status) {
        this.status = status;
    }
    // </editor-fold>

    // <editor-fold desc="Domain methods">
    public DetailingClaim toDomainObject() {
        if (fileSets == null) {
            fileSets = new HashMap<>();
        }
        var fileSetsDomain = fileSets.entrySet().stream()
                .map(entry -> {
                    var key = entry.getKey();
                    var value = entry.getValue().toDomainObject();
                    return Map.entry(key, value);
                })
                .collect(Collectors.toMap(
                        e -> (String) e.getKey(),
                        e -> (FileSet) e.getValue()
                ));

        return DetailingClaim.builder(jobId, claimedByStaffUsername)
                .floortechJobNumber(floortechJobNumber)
                .status(status)
                .jobClientId(jobClientId)
                .jobClientName(jobClientName)
                .jobEngineerId(jobEngineerId)
                .jobEngineerName(jobEngineerName)
                .claimedByStaffId(claimedByStaffId)
                .claimedAt(createdDate)
                .fileSets(fileSetsDomain)
                .build();
    }

    public DetailingClaimEntity updateFrom(DetailingClaim detailingClaim) {
        // jobId, staffUserId, staffUsername, should not be changed
        var jobNumber = toJobNumber.apply(detailingClaim);
        var status = toStatus.apply(detailingClaim);
        var clientId = toClientId.apply(detailingClaim);
        var clientName = toClientName.apply(detailingClaim);
        var engineerId = toEngineerId.apply(detailingClaim);
        var engineerName = toEngineerName.apply(detailingClaim);
        var fileSets = toFileSets.apply(detailingClaim);
        Map<String, FileSetEntity> fileSetEntities = Map.of();
        if (fileSets != null) {
            fileSetEntities = fileSets.entrySet().stream()
                    .map(entry -> {
                        var key = entry.getKey();
                        var value = FileSetEntity.fromDomainObject(entry.getValue());
                        return Map.entry(key, value);
                    })
                    .collect(Collectors.toMap(
                            e -> (String)e.getKey(),
                            e -> (FileSetEntity)e.getValue()
                    ));
        }

        if (jobNumber != null && jobNumber > 20000) this.floortechJobNumber = jobNumber;
        if (status != null) this.status = status;
        if (clientId != null && !clientId.isEmpty()) this.jobClientId = clientId;
        if (clientName != null && !clientName.isEmpty()) this.jobClientName = clientName;
        if (engineerId != null && !engineerId.isEmpty()) this.jobEngineerId = engineerId;
        if (engineerName != null && !engineerName.isEmpty()) this.jobEngineerName = engineerName;
        this.fileSets = fileSetEntities;
        return this;
    }
    // </editor-fold>

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetailingClaimEntity that = (DetailingClaimEntity) o;
        return id.equals(that.id) && jobId.equals(that.jobId) && claimedByStaffUsername.equals(that.claimedByStaffUsername) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, jobId, floortechJobNumber, jobClientId, jobClientName, jobEngineerId, jobEngineerName, claimedByStaffUsername, claimedByStaffId, fileSets, status);
    }

    @Override
    public String toString() {
        return "DetailingClaimEntity{" +
                "id='" + id + '\'' +
                ", jobId='" + jobId + '\'' +
                ", floortechJobNumber=" + floortechJobNumber +
                ", jobClientId='" + jobClientId + '\'' +
                ", jobClientName='" + jobClientName + '\'' +
                ", jobEngineerId='" + jobEngineerId + '\'' +
                ", jobEngineerName='" + jobEngineerName + '\'' +
                ", claimedByStaffUsername='" + claimedByStaffUsername + '\'' +
                ", claimedByStaffId='" + claimedByStaffId + '\'' +
                ", fileSets=" + fileSets +
                ", status=" + status +
                ", createdByUser='" + createdByUser + '\'' +
                ", createdDate=" + createdDate +
                ", modifiedByUser='" + modifiedByUser + '\'' +
                ", lastModifiedDate=" + lastModifiedDate +
                ", version=" + version +
                ", deleted=" + deleted +
                '}';
    }
}
