package nl.freshcoders.fit.tracing.span;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Span {

    public String parentSpanId;

    public List<Span> children = new ArrayList<>();

    public String traceId;
    public String operationName;

    public String spanId;

    public String className;

    public String methodName;

    public Long duration;

    public String host;

    public Trace  trace;

    public List<HashMap<String, String>> tags;

    public Span(String parentSpanId, String traceId, String spanId, String operationName, String className, String methodName, Long duration, String host, List tags) {
        this.parentSpanId = parentSpanId;
        this.traceId = traceId;
        this.operationName = operationName;
        this.spanId = spanId;
        this.className = className;
        this.methodName = methodName;
        this.duration = duration;
        this.host = host;
        this.tags = tags;
    }

    public Boolean isTopLevelSpan() {
        return this.parentSpanId == null;
    }

    public static Span fromElasticMap(Map<String, Object> document) {
        List references = (ArrayList) document.getOrDefault("references", new ArrayList<HashMap<String, String>>());
        String parentTrace = null;
        String parentSpan = null;
        for (Map<String, String> reference : (List<Map>) references) {
            String refType = (String) reference.getOrDefault("refType", "");
            if ("CHILD_OF".equals(refType)) {
                parentTrace = reference.get("traceID");
                parentSpan = reference.get("spanID");
            }
        }
        String traceId = (String) document.getOrDefault("traceID", "MISSING_TRACE");
        String spanId = (String) document.getOrDefault("spanID", "MISSING_SPAN");
        String operationName = (String) document.getOrDefault("operationName", "NO_OPERATION");

        String className = "NO_CLASS";
        String methodName = "NO_METHOD";
        List tags = (ArrayList<HashMap<String, String>>) document.getOrDefault("tags", new ArrayList<HashMap<String, String>>());

        for (Map tag : (List<HashMap<String, String>>) tags) {
            if (tag.get("key").equals("code.namespace")) {
                className = (String) tag.get("value");
            }
            if (tag.get("key").equals("code.function")) {
                methodName = (String) tag.get("value");
            }

        }
        Long duration = (Long) document.getOrDefault(document.get("duration"), 0L);

        String host = (String) ((Map<String, String>) document.get("process")).get("serviceName");
        return new Span(parentSpan, traceId, spanId, operationName, className, methodName, duration, host, tags);
    }

    public void addChild(Span span) {
        children.add(span);
    }


    public void printTrace() {
        System.out.println("Trace: " + traceId);
        recurseChildren("");
    }

    public void recurseChildren(String indent) {
        System.out.print(indent + " > " + operationName + " - " + spanId + " " + className + "#" + methodName);
        indent += "  ";
        for (Span child : children) {
            System.out.println("");
            child.recurseChildren(indent);
        }
    }

}