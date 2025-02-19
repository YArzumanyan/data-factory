package cz.cuni.mff.arzumany.metadata_store.model;

import java.io.Serializable;
import java.util.Objects;

public class Pipeline_Id implements Serializable {

    private String pipelineId;
    private int version;

    // Default constructor
    public Pipeline_Id() {}

    public Pipeline_Id(String pipelineId, int version) {
        this.pipelineId = pipelineId;
        this.version = version;
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

    // equals and hashCode based on both fields
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pipeline_Id that)) return false;
        return version == that.version &&
                Objects.equals(pipelineId, that.pipelineId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipelineId, version);
    }
}
