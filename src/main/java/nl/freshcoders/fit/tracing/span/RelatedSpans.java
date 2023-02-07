package nl.freshcoders.fit.tracing.span;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
            parent = findAncestorOnDifferentHost(trace, source, parent);
        }
        return parent;
    }

    public static Boolean hasServerSpanKindTag(Span s) {
        for (HashMap<String, String> tag : s.tags) {
            if (tag.get("key").equals("span.kind") && tag.get("value").equals("server")) {
                return true;
            }
        }
        return false;
    }

    public static List<RelatedSpans> findCommunicationRelation(Trace trace) {
        List<RelatedSpans> relatedSpans = new ArrayList<>();

        List<Span> serverSpans = getChildWithServerSpanKindTag(trace.topLevelSpan);
        if (serverSpans.size() == 0) {
            return relatedSpans;
        }


        serverSpans.forEach(
                span -> relatedSpans.add(findAncestorRelation(trace, span, span))
        );

        return relatedSpans;
    }

    public static List<Span> getChildWithServerSpanKindTag(Span span) {
        if (hasServerSpanKindTag(span))
            return Arrays.asList(span);

        List<Span> spans = new ArrayList<>();
        for (Span child : span.children) {
            if (hasServerSpanKindTag(child))
                spans.add(child);
            else
                spans.addAll(getChildWithServerSpanKindTag(child));
        }

        return spans;
    }
}
