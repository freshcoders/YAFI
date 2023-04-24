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

    public Integer duration;

    public Long start;

    public String host;

    public Trace trace;

    public Map<String, String> tags;

    public Span(String parentSpanId, String traceId, String spanId, String operationName, String className, String methodName, Integer duration, Long start, String host, Map tags) {
        this.parentSpanId = parentSpanId;
        this.traceId = traceId;
        this.operationName = operationName;
        this.spanId = spanId;
        this.className = className;
        this.methodName = methodName;
        this.duration = duration;
        this.start = start;
        this.host = host;
        this.tags = tags;
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

        Map tags = new HashMap<String, String>();

        if (document.get("tags") != null) {
            ((ArrayList<HashMap<String, String>>) document.get("tags")).forEach(tagEntry ->
                    tags.put(tagEntry.get("key"), tagEntry.get("value"))
                    );
        }

        String className = tags.getOrDefault("code.namespace", "NO_CLASS").toString();
        String methodName = tags.getOrDefault("code.function", "NO_METHOD").toString();

        // this is ZooKeeper specific, it would be better to instrument this from the application and set code.namespace/function:
        if (operationName.contains(".")) {
            String[] parts = operationName.split("\\.");
            className = parts[0];
            methodName = parts[1];
        }

        Integer duration = (int) document.getOrDefault("duration", 0);
        Long start = (Long) document.getOrDefault("startTimeMillis", 0L);

        String host = ((Map<String, String>) document.get("process")).get("serviceName");
        return new Span(parentSpan, traceId, spanId, operationName, className, methodName, duration, start, host, tags);
    }

    public Boolean isTopLevelSpan() {
        return this.parentSpanId == null;
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