package nl.freshcoders.fit.plan.workload.states;

import nl.freshcoders.fit.Orchestrator;
import nl.freshcoders.fit.generator.ClockPlanGenerator;
import nl.freshcoders.fit.generator.CommunicatorPerturbationGeneration;
import nl.freshcoders.fit.generator.GenericFaultGenerator;
import nl.freshcoders.fit.generator.PerturbationGenerator;
import nl.freshcoders.fit.tracing.ElasticSource;
import nl.freshcoders.fit.tracing.TraceReader;
import nl.freshcoders.fit.tracing.span.RelatedSpans;

import java.util.List;
import java.util.Set;

public class AnalyseReferenceExecutionState implements WorkloadState {
    private boolean analysisComplete = false;
    @Override
    public boolean isReady() {
        return analysisComplete;
    }

    @Override
    public void execute(Orchestrator orchestrator) {
        CommunicatorPerturbationGeneration commFaults =
                new CommunicatorPerturbationGeneration(orchestrator.getFailurePlanRunner().getFailurePlan());
        ClockPlanGenerator clockFaults =
                new ClockPlanGenerator(orchestrator.getFailurePlanRunner().getFailurePlan());
        GenericFaultGenerator gg = new
                GenericFaultGenerator(orchestrator.getFailurePlanRunner().getFailurePlan());
        PerturbationGenerator pb = new PerturbationGenerator();
        long referenceStartTime = orchestrator.getFailurePlanRunner().getPlanState().getStartTime();
        long referenceEndTime = orchestrator.getFailurePlanRunner().getPlanState().getEndTime();
        orchestrator.getFailurePlanRunner().writeOut();
        orchestrator.getFailurePlanRunner().cyclePlanState();

        try {
            // only enable this if we expect a fixed delay in the source we use after (tracing)
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        orchestrator.getFailurePlanRunner().getPlanState().setPlanReference("analyse-reference");
        ElasticSource elasticSource = new ElasticSource("localhost", 9200);
        TraceReader reader = new TraceReader(elasticSource);
        reader.setTimeFrame(referenceStartTime, referenceEndTime);
        List<RelatedSpans> relatedSpansList = reader.getCommunicationTraces();
        Set<String> uniqueSpans = reader.getUniqueSpans();
        pb.registerGenerator(clockFaults);
        pb.registerGenerator(commFaults);
        pb.registerGenerator(gg);

        for (RelatedSpans relatedSpans : relatedSpansList) {
            commFaults.generateFromSpanRelation(relatedSpans);
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        clockFaults.generateFixedClockPlan(String.valueOf(System.currentTimeMillis()));

        uniqueSpans.forEach(methodLoc -> {
            String clazz = methodLoc.split("#")[0];
            String method = methodLoc.split("#")[1];
            gg.generateInstantFaultAllHosts("exception", clazz, method, "RuntimeException");
        });


//        clockFaults.generateSkews("abstract java.time.Clock", "millis");
        orchestrator.generations = pb.accumulatePlans();
        analysisComplete = true;
    }
}
