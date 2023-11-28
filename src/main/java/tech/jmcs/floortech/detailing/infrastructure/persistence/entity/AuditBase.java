package tech.jmcs.floortech.detailing.infrastructure.persistence.entity;

import org.springframework.data.annotation.*;
import java.util.Date;

public abstract class AuditBase {
    @CreatedBy
    String createdByUser;
    @CreatedDate
    Date createdDate;
    @LastModifiedBy
    String modifiedByUser;
    @LastModifiedDate
    Date lastModifiedDate;
    @Version
    Long version;
    Boolean deleted = false;

    public AuditBase() {
    }

    public AuditBase(String createdByUser, Date createdDate, String modifiedByUser, Date lastModifiedDate, Long version, Boolean deleted) {
        this.createdByUser = createdByUser;
        this.createdDate = createdDate;
        this.modifiedByUser = modifiedByUser;
        this.lastModifiedDate = lastModifiedDate;
        this.version = version;
        this.deleted = deleted;
    }

    public String getCreatedByUser() {
        return createdByUser;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public String getModifiedByUser() {
        return modifiedByUser;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Long getVersion() {
        return version;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setCreatedByUser(String createdByUser) {
        this.createdByUser = createdByUser;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public void setModifiedByUser(String modifiedByUser) {
        this.modifiedByUser = modifiedByUser;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
