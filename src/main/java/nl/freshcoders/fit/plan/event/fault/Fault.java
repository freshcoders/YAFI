package nl.freshcoders.fit.plan.event.fault;

import java.util.Map;

public class Fault {

    private String type;
    private String config;

    private Occurrence occurrence;

    public Fault(String type, String config, Occurrence occurence) {
        this.type = type;
        this.config = config;
        this.occurrence = occurence;
    }

    public Occurrence getOccurrence() {
        return occurrence;
    }

    public void setOccurrence(Occurrence occurrence) {
        this.occurrence = occurrence;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "Fault{" +
                "type='" + type + '\'' +
                ", config='" + config + '\'' +
                ", occurrence='" + occurrence + '\'' +
                '}';
    }

    public Map<String, Object> mapData() {
        if (config != null)
            return Map.of(
                    "type", getType(),
                    "config", getConfig(),
                    "occurrence", getOccurrence().mapData()
            );
        else
            return Map.of(
                    "type", getType(),
                    "occurrence", getOccurrence().mapData()
            );
    }
}
