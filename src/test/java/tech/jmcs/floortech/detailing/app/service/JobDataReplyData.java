package tech.jmcs.floortech.detailing.app.service;

import org.springframework.amqp.core.MessageProperties;
import java.util.Objects;

public class JobDataReplyData {
    String uuid;
    Integer jobNumber;
    String replyTo;
    MessageProperties messageProperties;
    public JobDataReplyData(String uuid, Integer jobNumber, String replyTo, MessageProperties messageProperties) {
        this.uuid = uuid;
        this.jobNumber = jobNumber;
        this.replyTo = replyTo;
        this.messageProperties = messageProperties;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getJobNumber() {
        return jobNumber;
    }

    public void setJobNumber(Integer jobNumber) {
        this.jobNumber = jobNumber;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public MessageProperties getMessageProperties() {
        return messageProperties;
    }

    public void setMessageProperties(MessageProperties messageProperties) {
        this.messageProperties = messageProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobDataReplyData that = (JobDataReplyData) o;
        return Objects.equals(uuid, that.uuid) && Objects.equals(jobNumber, that.jobNumber) && Objects.equals(replyTo, that.replyTo) && Objects.equals(messageProperties, that.messageProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, jobNumber, replyTo, messageProperties);
    }

    @Override
    public String toString() {
        return "JobDataReplyData{" +
                "uuid='" + uuid + '\'' +
                ", jobNumber=" + jobNumber +
                ", replyTo='" + replyTo + '\'' +
                ", messageProperties=" + messageProperties +
                '}';
    }
}
