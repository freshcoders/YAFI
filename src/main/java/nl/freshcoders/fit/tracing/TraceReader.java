package nl.freshcoders.fit.tracing;

import nl.freshcoders.fit.Orchestrator;
import nl.freshcoders.fit.plan.runner.FailurePlanRunner;
import nl.freshcoders.fit.plan.runner.PlanState;
import nl.freshcoders.fit.tracing.span.RelatedSpans;
import nl.freshcoders.fit.tracing.span.Span;
import nl.freshcoders.fit.tracing.span.Trace;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TraceReader {

    private final Set<String> traceList = new HashSet<>();
    private final ElasticSource elasticSource;

    Integer communicatingTraceCount = 0;

    public TraceReader(ElasticSource elasticSource) {
        this.elasticSource = elasticSource;
    }

    public List<RelatedSpans> getCommunicationTraces() {
        prefetch();
        communicatingTraceCount = 0;
        List<RelatedSpans> relatedSpans = new ArrayList<>();
        for (String traceId : traceList) {
            Trace trace = elasticSource.buildTrace(traceId);
            List<RelatedSpans> communicationRelations = RelatedSpans.findCommunicationRelation(trace);
            // we found a set of spans, related by communication
            // we can now analyse this set and turn it into faults
            // is there merit in bundling? for now just append all to the main list..
            relatedSpans.addAll(communicationRelations.stream().filter(c -> c.origin != null).collect(Collectors.toList()));
        }
        return relatedSpans;
    }

    public Set<String> getUniqueSpans() {
        Set<String> uniqueSpans = new HashSet<>();
        prefetch();
        for (String traceId : traceList) {
            Trace trace = elasticSource.buildTrace(traceId);
            if (trace.topLevelSpan != null) {
                if (!trace.topLevelSpan.className.equals("NO_CLASS")) {
                    uniqueSpans.add(trace.topLevelSpan.className + "#" + trace.topLevelSpan.methodName);
                }
                uniqueSpans.addAll(trace.topLevelSpan.children.stream()
                        .filter(span -> !span.className.equals("NO_CLASS") && !span.methodName.equals("NO_METHOD"))
                        .map(span -> span.className + "#" + span.methodName)
                        .collect(Collectors.toList()));
            }
        }
        return uniqueSpans;
    }

    private Set<String> prefetch() {
        int retries = 0;
        if (traceList.isEmpty()) {
            while (elasticSource.getTraceList().isEmpty() && retries++ <= 0) {
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            traceList.addAll(elasticSource.getTraceList());
        }
        return traceList;
    }

    public List<Trace> getBasicCommunicationTraces() {
        prefetch();
        List<Trace> allTraces = this.traceList.stream().map(elasticSource::buildTrace).collect(Collectors.toList());
        return RelatedSpans.findBasicCommunicationTraces(allTraces);
    }

    public Set<String> getTraceList() {
        return prefetch();
    }

    public void setTimeFrame(long start, long end) {
        System.out.println("reader set to " + start + "===" + end);
        if (!elasticSource.getStart().equals(start) || !elasticSource.getEnd().equals(end)) {
            this.traceList.clear();
        }
        elasticSource.bumpStart(start);
        elasticSource.bumpEnd(end);
    }

    public void dumpExecutionAnalytics(FailurePlanRunner failurePlanRunner) {
        String generationPath = System.getProperty("user.dir") + "/log/" + Orchestrator.getInstance().getRunId();
        Path path = Paths.get(generationPath);
        List<Map<String, Object>> ptiList = new ArrayList<>();
        try (FileWriter fw = new FileWriter(path.toAbsolutePath() + "/" + "analytics.yml")) {
            for (PlanState executedPlanState : failurePlanRunner.getExecutedPlanStates()) {
                System.out.println("writing analy:" + executedPlanState.getPlanReference());
                long start = executedPlanState.getStartTime();
                long end = executedPlanState.getEndTime();

                if (start >= end) {
                    if (executedPlanState.getPlanReference().contains("analyse")) {
                        System.out.println("analyse..");
                        ptiList.add(
                                Map.of(
                                        "reference", executedPlanState.getPlanReference(),
                                        "start", executedPlanState.getStartTime()
                                ));
                    }
                    continue;
                }
                setTimeFrame(start, end);
                prefetch();

                Integer traceCount = getTraceList().size();
                List<String> backendLatency = getTags("backend.latency");
                Integer commCount = getBasicCommunicationTraces().size();
                Map<Long, Long> timings = getTimeCounters();
                System.out.println(commCount + "/" + (traceCount - commCount) + " comm/internal traces");



                ptiList.add(
                        Map.of(
                                "reference", executedPlanState.getPlanReference(),
                                "start", executedPlanState.getStartTime(),
                                "comm", commCount,
                                "internal", Math.max(traceCount-commCount, 0),
                                "backend_latency", Integer.parseInt(backendLatency.stream().findFirst().orElse("0")),
                                "timings", timings
                        ));
            }

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
            Yaml yaml = new Yaml(options);
            yaml.dump(ptiList, fw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Long, Long> getTimeCounters() {
        prefetch();

        Stream<Span> spanStream = traceList.stream().flatMap((s) -> {
            Trace trace = elasticSource.buildTrace(s);
            return trace.getSpans().values().stream();
        });

        return spanStream
                .filter(span -> !span.tags.getOrDefault("backend.latency", "0").equals("0"))
                .collect(Collectors.toMap(
                        span -> span.start, span -> Long.parseLong(span.tags.get("backend.latency")), (n1, n2) -> Math.max(n1,n2)
                ));

    }

    public List<String> getTags(String tagName) {
        prefetch();

        Stream<Span> spanStream = traceList.stream().flatMap((s) -> {
            Trace trace = elasticSource.buildTrace(s);
            return trace.getSpans().values().stream();
        });

        return spanStream
                .filter(span ->
                        span.tags.get(tagName) != null
                )
                .map(span -> span.tags.get(tagName))
                .collect(Collectors.toList());
    }
}

