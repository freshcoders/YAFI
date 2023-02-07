package nl.freshcoders.fit.plan.event.trigger;

import java.util.HashMap;
import java.util.Map;

public class TriggerValue {

    private final String source;
    private final String type;
    protected Map<String, String> values;

    public TriggerValue(String source, String type, String triggerVal) {
        this.type = type;
        this.source = source;
        values = new HashMap<>();

        switch (type) {
            case "trace":
                String[] parts = triggerVal.split("#", 2);
                values.put("traceClass", parts[0]);
                values.put("traceMethod", parts[1]);
                break;
        }
        switch (type) {
            case "clock":
                values.put("time", triggerVal);
                break;
        }
    }

    public String type() { return type; }

    public String get(String key) {
        return values.getOrDefault(key, "");
    }

    public String getSource() {
        return source;
    }
}
