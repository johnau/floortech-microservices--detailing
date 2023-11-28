package tech.jmcs.floortech.detailing.domain.model.detailingclaim;

import tech.jmcs.floortech.detailing.domain.configs.DetailingStatus;
import tech.jmcs.floortech.detailing.domain.model.fileset.FileSet;
import tech.jmcs.floortech.detailing.domain.model.fileset.FileSetFacade;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static tech.jmcs.floortech.detailing.domain.model.fileset.FileSetFacade.toFileSetId;

public class DetailingClaim {
    final String jobId;
    final Integer floortechJobNumber;
    final String jobClientId;
    final String jobClientName;
    final String jobEngineerId;
    final String jobEngineerName;
    final String claimedByStaffUsername;
    final String claimedByStaffId;
    final Date claimedAt;
    final Map<String, FileSet> fileSets; // fileSets by ID (FileSet.id)
    final DetailingStatus status;

    // <editor-fold desc="Immutable Modifier methods">
    // TODO: Job ID is FloortechJobNumber, can remove
    public DetailingClaim withFloortechJobNumber(Integer newValue) {
        return DetailingClaim.builder(jobId, claimedByStaffUsername)
                .floortechJobNumber(newValue)
                .jobClientId(jobClientId)
                .jobClientName(jobClientName)
                .jobEngineerId(jobEngineerId)
                .jobEngineerName(jobEngineerName)
                .claimedByStaffId(claimedByStaffId)
                .claimedAt(claimedAt)
                .fileSets(fileSets)
                .status(status)
                .build();
    }

    public DetailingClaim withClientId(String newValue) {
        return DetailingClaim.builder(jobId, claimedByStaffUsername)
                .floortechJobNumber(floortechJobNumber)
                .jobClientId(newValue)
                .jobClientName(jobClientName)
                .jobEngineerId(jobEngineerId)
                .jobEngineerName(jobEngineerName)
                .claimedByStaffId(claimedByStaffId)
                .claimedAt(claimedAt)
                .fileSets(fileSets)
                .status(status)
                .build();
    }

    public DetailingClaim withClientName(String newValue) {
        return DetailingClaim.builder(jobId, claimedByStaffUsername)
                .floortechJobNumber(floortechJobNumber)
                .jobClientId(jobClientId)
                .jobClientName(newValue)
                .jobEngineerId(jobEngineerId)
                .jobEngineerName(jobEngineerName)
                .claimedByStaffId(claimedByStaffId)
                .claimedAt(claimedAt)
                .fileSets(fileSets)
                .status(status)
                .build();
    }

    public DetailingClaim withEngineerId(String newValue) {
        return DetailingClaim.builder(jobId, claimedByStaffUsername)
                .floortechJobNumber(floortechJobNumber)
                .jobClientId(jobClientId)
                .jobClientName(jobClientName)
                .jobEngineerId(newValue)
                .jobEngineerName(jobEngineerName)
                .claimedByStaffId(claimedByStaffId)
                .claimedAt(claimedAt)
                .fileSets(fileSets)
                .status(status)
                .build();
    }

    public DetailingClaim withEngineerName(String newValue) {
        return DetailingClaim.builder(jobId, claimedByStaffUsername)
                .floortechJobNumber(floortechJobNumber)
                .jobClientId(jobClientId)
                .jobClientName(jobClientName)
                .jobEngineerId(jobEngineerId)
                .jobEngineerName(newValue)
                .claimedByStaffId(claimedByStaffId)
                .claimedAt(claimedAt)
                .fileSets(fileSets)
                .status(status)
                .build();
    }

    public DetailingClaim withStatus(DetailingStatus newValue) {
        return DetailingClaim.builder(jobId, claimedByStaffUsername)
                .floortechJobNumber(floortechJobNumber)
                .jobClientId(jobClientId)
                .jobClientName(jobClientName)
                .jobEngineerId(jobEngineerId)
                .jobEngineerName(jobEngineerName)
                .claimedByStaffId(claimedByStaffId)
                .claimedAt(claimedAt)
                .fileSets(fileSets)
                .status(newValue)
                .build();
    }

    public DetailingClaim withFileSets(Map<String, FileSet> newValue) {
        if (newValue == null) newValue = new HashMap<>();
        return DetailingClaim.builder(jobId, claimedByStaffUsername)
                .floortechJobNumber(floortechJobNumber)
                .jobClientId(jobClientId)
                .jobClientName(jobClientName)
                .jobEngineerId(jobEngineerId)
                .jobEngineerName(jobEngineerName)
                .claimedByStaffId(claimedByStaffId)
                .claimedAt(claimedAt)
                .fileSets(newValue)
                .status(status)
                .build();
    }

    public DetailingClaim addFileSet(FileSet newFileSet) {
        var fileSetId = toFileSetId.apply(newFileSet);
        if (fileSets.containsKey(fileSetId)) {
            throw new RuntimeException("An file set exists with the same label, must check first");
        }
        var updatedFileSets = new HashMap<>(fileSets);
        updatedFileSets.putIfAbsent(fileSetId, newFileSet);
        return DetailingClaim.builder(jobId, claimedByStaffUsername)
                .floortechJobNumber(floortechJobNumber)
                .jobClientId(jobClientId)
                .jobClientName(jobClientName)
                .jobEngineerId(jobEngineerId)
                .jobEngineerName(jobEngineerName)
                .claimedByStaffId(claimedByStaffId)
                .claimedAt(claimedAt)
                .fileSets(updatedFileSets)
                .status(status)
                .build();
    }

    public DetailingClaim verifyAndStart() throws Exception {
        if (this.jobId == null) {
            throw new Exception("Missing jobId");
        }
        if (this.floortechJobNumber == null) {
            throw new Exception("Missing floortechJobId");
        }
        if (this.claimedByStaffUsername == null) {
            throw new Exception("Missing claimedByStaffUsername");
        }
        if (this.status == null) {
            throw new Exception("Missing status");
        }

        return this.withStatus(DetailingStatus.STARTED);
    }

    public DetailingClaim verifyAndComplete() throws Exception {
        if (this.status != DetailingStatus.STARTED) {
            throw new Exception("Can't complete a job that is not active");
        }
        if (this.fileSets.isEmpty()) {
            throw new Exception("Can't complete a job with no file sets");
        }
        return this.withStatus(DetailingStatus.COMPLETED);
    }

    public DetailingClaim verifyAndCancel() throws Exception {
        if (this.status != DetailingStatus.STARTED && this.status != DetailingStatus.PAUSED && this.status != DetailingStatus.UNVERIFIED) {
            throw new Exception("Can not cancel job with status: " + this.status);
        }

        return this.withStatus(DetailingStatus.CANCELLED);
    }

    public DetailingClaim verifyAndPause() throws Exception {
        if (this.status != DetailingStatus.STARTED) {
            throw new Exception("Can not pause job with status: " + this.status);
        }
        return this.withStatus(DetailingStatus.PAUSED);
    }

    /**
     * Allows updating with null or empty values.
     * Returns a new, updated DetailingClaim instance
     * @param newValues DetailingClaim object with values to overwrite
     * @return new, updated DetailingClaim instance
     */
    public DetailingClaim forceUpdate(DetailingClaim newValues) {
        var _floortechJobNumber = newValues.floortechJobNumber;
        var _jobClientId = newValues.jobClientId;
        var _jobClientName = newValues.jobClientName;
        var _jobEngineerId = newValues.jobEngineerId;
        var _jobEngineerName = newValues.jobEngineerName;
        var _claimedByStaffId = newValues.claimedByStaffId;
        var _fileSets = newValues.fileSets;
        var _status = newValues.status;
        return DetailingClaim.builder(jobId, claimedByStaffUsername)
                .claimedAt(claimedAt)
                .floortechJobNumber(_floortechJobNumber)
                .jobClientId(_jobClientId)
                .jobClientName(_jobClientName)
                .jobEngineerId(_jobEngineerId)
                .jobEngineerName(_jobEngineerName)
                .claimedByStaffId(_claimedByStaffId)
                .fileSets(_fileSets)
                .status(_status)
                .build();
    }

    /**
     * Updates a DetailingClaim object with new values.
     * Does not overwrite values if provided with null or empty value.
     * @param newValues
     * @return new, updated Detailing Claim object
     */
    public DetailingClaim updateIgnoringNullsAndEmpty(DetailingClaim newValues) {
//      Fields never change: jobId, claimedByStaffUsername, claimedAt
        var _floortechJobNumber = newValues.floortechJobNumber != null && newValues.floortechJobNumber > 0 ? newValues.floortechJobNumber : this.floortechJobNumber;
        var _jobClientId = newValues.jobClientId != null && !newValues.jobClientId.isBlank() ? newValues.jobClientId : this.jobClientId;
        var _jobClientName = newValues.jobClientName != null && !newValues.jobClientName.isBlank() ? newValues.jobClientName : this.jobClientName;
        var _jobEngineerId = newValues.jobEngineerId !=  null && !newValues.jobEngineerId.isBlank() ? newValues.jobEngineerId : this.jobEngineerId;
        var _jobEngineerName = newValues.jobEngineerName != null && !newValues.jobEngineerName.isBlank() ? newValues.jobEngineerName : this.jobEngineerName;
        var _claimedByStaffId = newValues.claimedByStaffId != null && !newValues.claimedByStaffId.isBlank() ? newValues.claimedByStaffId : this.claimedByStaffId;
        var _fileSets = newValues.fileSets != null && !newValues.fileSets.isEmpty() ? newValues.fileSets : this.fileSets;
        var _status = newValues.status != null ? newValues.status : this.status;
        return DetailingClaim.builder(jobId, claimedByStaffUsername)
                .claimedAt(claimedAt)
                .floortechJobNumber(_floortechJobNumber)
                .jobClientId(_jobClientId)
                .jobClientName(_jobClientName)
                .jobEngineerId(_jobEngineerId)
                .jobEngineerName(_jobEngineerName)
                .claimedByStaffId(_claimedByStaffId)
                .fileSets(_fileSets)
                .status(_status)
                .build();
    }
    // </editor-fold>

    // <editor-fold desc="Builder components (Constructor, Builder Class, Static method)">
    public static DetailingClaim newUnverifiedClaim(String jobId, String username) {
        return DetailingClaim.builder(jobId, username)
                .status(DetailingStatus.UNVERIFIED)
                .build();
    }

    public static DetailingClaimBuilder builder(String jobId, String username) {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(username);
        return new DetailingClaimBuilder(jobId, username);
    }

    public DetailingClaim(DetailingClaimBuilder builder) {
        this.jobId = builder.jobId;
        this.floortechJobNumber = builder.floortechJobNumber;
        this.jobClientId = builder.jobClientId;
        this.jobClientName = builder.jobClientName;
        this.jobEngineerId = builder.jobEngineerId;
        this.jobEngineerName = builder.jobEngineerName;
        this.claimedByStaffUsername = builder.claimedByStaffUsername;
        this.claimedByStaffId = builder.claimedByStaffId;
        this.claimedAt = builder.claimedAt;
        this.fileSets = builder.fileSets != null ? builder.fileSets : new HashMap();
        this.status = builder.status;
    }

    public static class DetailingClaimBuilder {
        private String jobId;
        private Integer floortechJobNumber;
        private String jobClientId;
        private String jobClientName;
        private String jobEngineerId;
        private String jobEngineerName;
        private String claimedByStaffUsername;
        private String claimedByStaffId;
        private Date claimedAt;
        private Map<String, FileSet> fileSets; // fileSets by ID (FileSet.id)
        private DetailingStatus status;
        public DetailingClaimBuilder(String jobId, String username) {
            Objects.requireNonNull(jobId);
            Objects.requireNonNull(username);
            this.jobId = jobId;
            this.claimedByStaffUsername = username;
            this.status = DetailingStatus.UNVERIFIED;
        }

        public DetailingClaimBuilder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public DetailingClaimBuilder floortechJobNumber(Integer floortechJobNumber) {
            this.floortechJobNumber = floortechJobNumber;
            return this;
        }

        public DetailingClaimBuilder jobClientId(String jobClientId) {
            this.jobClientId = jobClientId;
            return this;
        }

        public DetailingClaimBuilder jobClientName(String jobClientName) {
            this.jobClientName = jobClientName;
            return this;
        }

        public DetailingClaimBuilder jobEngineerId(String jobEngineerId) {
            this.jobEngineerId = jobEngineerId;
            return this;
        }

        public DetailingClaimBuilder jobEngineerName(String jobEngineerName) {
            this.jobEngineerName = jobEngineerName;
            return this;
        }

        public DetailingClaimBuilder claimedByStaffUsername(String claimedByStaffUsername) {
            this.claimedByStaffUsername = claimedByStaffUsername;
            return this;
        }

        public DetailingClaimBuilder claimedByStaffId(String claimedByStaffId) {
            this.claimedByStaffId = claimedByStaffId;
            return this;
        }

        public DetailingClaimBuilder claimedAt(Date claimedAt) {
            this.claimedAt = claimedAt;
            return this;
        }

        public DetailingClaimBuilder fileSets(Map<String, FileSet> fileSets) {
            this.fileSets = fileSets;
            return this;
        }

        public DetailingClaimBuilder status(DetailingStatus status) {
            this.status = status;
            return this;
        }

        public DetailingClaim build() {
            Objects.requireNonNull(this.jobId);
            Objects.requireNonNull(this.claimedByStaffUsername);
            Objects.requireNonNull(this.status);
            return new DetailingClaim(this);
        }
    }
    // </editor-fold>

    @Override
    public String toString() {
        return "DetailingClaim{" +
                ", jobId='" + jobId + '\'' +
                ", floortechJobNumber=" + floortechJobNumber +
                ", jobClientId='" + jobClientId + '\'' +
                ", jobClientName='" + jobClientName + '\'' +
                ", jobEngineerId='" + jobEngineerId + '\'' +
                ", jobEngineerName='" + jobEngineerName + '\'' +
                ", claimedByStaffUsername='" + claimedByStaffUsername + '\'' +
                ", claimedByStaffId='" + claimedByStaffId + '\'' +
                ", claimedAt=" + claimedAt +
                ", fileSets=" + fileSets +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetailingClaim that = (DetailingClaim) o;
        return jobId.equals(that.jobId) && Objects.equals(floortechJobNumber, that.floortechJobNumber) && Objects.equals(jobClientId, that.jobClientId) && Objects.equals(jobClientName, that.jobClientName) && Objects.equals(jobEngineerId, that.jobEngineerId) && Objects.equals(jobEngineerName, that.jobEngineerName) && claimedByStaffUsername.equals(that.claimedByStaffUsername) && Objects.equals(claimedByStaffId, that.claimedByStaffId) && claimedAt.equals(that.claimedAt) && Objects.equals(fileSets, that.fileSets) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, floortechJobNumber, jobClientId, jobClientName, jobEngineerId, jobEngineerName, claimedByStaffUsername, claimedByStaffId, claimedAt, fileSets, status);
    }
}