package nl.freshcoders.fit.tracing;

import java.io.Serializable;

public class PlanTraceInfo implements Serializable {
    private String ref;
        private Long timestamp;
    private Integer comm;
    private Integer internal;

    public PlanTraceInfo(String ref, Long timestamp, Integer comm, Integer internal) {
        this.ref = ref;
        this.timestamp = timestamp;
        this.comm = comm;
        this.internal = internal;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getComm() {
        return comm;
    }

    public void setComm(Integer comm) {
        this.comm = comm;
    }

    public Integer getInternal() {
        return internal;
    }

    public void setInternal(Integer internal) {
        this.internal = internal;
    }
}
