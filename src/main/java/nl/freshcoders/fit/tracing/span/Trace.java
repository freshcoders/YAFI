package nl.freshcoders.fit.tracing.span;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collection of all spans in a trace, manages relations.
 */
public class Trace {

    private Map<String, Span> spans = new HashMap();

    public Span topLevelSpan;
    public String traceId;

    public Integer spanCount = 0;

    public Map<String, List<String>> relationQueue = new ConcurrentHashMap<>();

    public Span find(String spanId) {
        Span span = spans.get(spanId);
        return span;
    }

    public Span findByRef(String ref) {
        Span span = spans.get(ref);
        return span;
    }

    public Span findParent(Span child) {
        Span span = find(child.parentSpanId);
        return span;
    }

    public void addSpan(Span span) {
        if (traceId == null) {
            traceId = span.traceId;
        }
        spans.put(span.spanId, span);
        spanCount++;
        // store the span parent-child relation, for resolving this when all spans are indexed
        if (span.parentSpanId != null) {
            List<String> children = relationQueue.getOrDefault(span.parentSpanId, new ArrayList<String>());
            children.add(span.spanId);
            relationQueue.put(span.parentSpanId, children);
        }

        if (span.isTopLevelSpan()) {
            topLevelSpan = span;
        }
    }

    public void resolveChildRelations() {
        for (Map.Entry<String, List<String>> relations : relationQueue.entrySet()) {
            String parentSpan = relations.getKey();
            List<String> childRefs = relations.getValue();

            Span parent = findByRef(parentSpan);
            if (parent == null) {
                continue;
            }
            for (int i = childRefs.size() - 1; i >= 0; i--) {
                Span child = findByRef(childRefs.get(i));
                parent.addChild(child);
                childRefs.remove(i);
                // race cond? (concurrency)
                if (childRefs.size() == 0) {
                    relationQueue.remove(parentSpan);
                }
            }
        }
        // Recurse into deeper layers, this logic might need to be changed if the parents
        // are added last, because then we can only do one layer at a time and this has
        // the worst complexity
        // however, we add all spans first currently, so they will always be found..
        // if we start running this "on the fly" the spans might get added later
        if (relationQueue.size() > 0) {
//            resolveChildRelations();
            System.out.println("ERROR, we still have uncoupled parents");
        }
    }

}
