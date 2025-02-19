package cz.cuni.mff.arzumany.metadata_store.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pipeline_versions")
@IdClass(Pipeline_Id.class)
public class PipelineVersion {

    @Id
    @Column(name = "pipeline_id", nullable = false)
    private String pipelineId;

    @Id
    @Column(name = "version", nullable = false)
    private int version;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "CLOB")
    private String payload;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public PipelineVersion() {}

    public PipelineVersion(String pipelineId, int version, String payload, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.pipelineId = pipelineId;
        this.version = version;
        this.payload = payload;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public String getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(String pipelineId) {
        this.pipelineId = pipelineId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
