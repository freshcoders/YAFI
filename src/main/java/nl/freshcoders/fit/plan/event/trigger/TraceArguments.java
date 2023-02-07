package nl.freshcoders.fit.plan.event.trigger;

import java.util.Map;
import java.util.Objects;

public class TraceArguments implements TriggerArguments {

    private String traceClass;
    private String traceMethod;

    public TraceArguments(String traceClass, String traceMethod) {
        this.traceClass = traceClass;
        this.traceMethod = traceMethod;
    }

    public String getTraceClass() {
        return traceClass;
    }

    public String getTraceMethod() {
        return traceMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceArguments that = (TraceArguments) o;
        return Objects.equals(traceClass, that.traceClass) && Objects.equals(traceMethod, that.traceMethod);
    }

    public boolean match(TriggerValue o) {
        return o.get("traceMethod").equals(getTraceMethod()) && o.get("traceClass").equals(getTraceClass());
    }

    @Override
    public Object mapData() {
        return Map.of(
                "traceClass", traceClass,
                "traceMethod", traceMethod
        );
    }

    @Override
    public String toString() {
        return "TraceArguments{" +
                "traceClass='" + traceClass + '\'' +
                ", traceMethod='" + traceMethod + '\'' +
                '}';
    }
}
