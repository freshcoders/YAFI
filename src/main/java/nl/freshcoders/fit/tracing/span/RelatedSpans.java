package nl.freshcoders.fit.tracing.span;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class RelatedSpans {
    public Span origin;
    public Span target;

    public RelatedSpans(Span origin, Span target) {

        this.origin = origin;
        this.target = target;
    }


    public static RelatedSpans findAncestorRelation(Trace trace, Span source, Span current) {
        Span parent = findAncestorOnDifferentHost(trace, source, source);
        return new RelatedSpans(parent, source);
    }

    public static Span findAncestorOnDifferentHost(Trace trace, Span source, Span current) {
        Span parent = trace.findParent(current);
        if (parent == null) {
            return null;
        }
        if (parent.methodName.equals("NO_METHOD") || source.host.equals(parent.host)) {
            // we are still on the same host, go deeper into the trace to find the other host
            parent = findAncestorOnDifferentHost(trace, source, parent);
        }
        return parent;
    }

    public static Boolean hasServerSpanKindTag(Span s) {
        if (s.tags.getOrDefault("span.kind", "").equals("server")) {
            return true;
        }
        return false;
    }

    public static List<RelatedSpans> findCommunicationRelation(Trace trace) {
        List<RelatedSpans> relatedSpans = new ArrayList<>();

        if (null == trace.topLevelSpan) {
            return Collections.emptyList();
        }
        List<Span> serverSpans = getChildWithServerSpanKindTag(trace.topLevelSpan);
        if (serverSpans.size() == 0) {
            return relatedSpans;
        }


        serverSpans.forEach(
                span -> relatedSpans.add(findAncestorRelation(trace, span, span))
        );

        return relatedSpans;
    }

    public static List<Trace> findBasicCommunicationTraces(List<Trace> traces) {
        return traces.stream().filter(RelatedSpans::isCommunicatingTrace).collect(Collectors.toList());
    }

    private static boolean isCommunicatingTrace(Trace trace) {
        if (null == trace.topLevelSpan || trace.spanCount.equals(1)) {
            return false;
        }


        List<Span> spans = trace.topLevelSpan.children.stream().filter(
                child -> trace.topLevelSpan.host.equals(child.host)
        ).collect(Collectors.toList());

        return !trace.spanCount.equals(spans.size() + 1);
    }

    public static List<Span> getChildWithServerSpanKindTag(Span span) {
        List<Span> spans = new ArrayList<>();
        if (hasServerSpanKindTag(span))
            spans.add(span);

        for (Span child : span.children) {
            if (hasServerSpanKindTag(child))
                spans.add(child);
            else
                spans.addAll(getChildWithServerSpanKindTag(child));
        }

        return spans;
    }
}
