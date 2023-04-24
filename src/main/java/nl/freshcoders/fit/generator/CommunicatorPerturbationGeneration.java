package nl.freshcoders.fit.generator;

import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.tracing.span.RelatedSpans;

import java.util.List;

public class CommunicatorPerturbationGeneration extends TargetedGenerator {

    public CommunicatorPerturbationGeneration(FailurePlan plan) {
        this.currentPlan = plan;
    }

    public void generateFromSpanRelation(RelatedSpans relatedSpans) {
        generateDelays(relatedSpans.origin.className, relatedSpans.origin.methodName, relatedSpans.origin.host);
        generateDelays(relatedSpans.origin.className, relatedSpans.origin.methodName, relatedSpans.target.host);
    }

    private void generateDelays(String clazz, String method, String host) {
        // TODO: create multiple plans from timeout settings or from default
        List<Integer> delays = generateDelayList();
        for (Integer delay : delays) {
            // for now, trigger all delays at the start
            generate("delay", clazz, method, delay.toString(), 0, host, true);
        }
    }

    /**
     * Create a delay list on the second scale.
     * @return
     */
    private List<Integer> generateDelayList() {
        // We use fixed timeout values for now
        int timeout = 10 * 1000;
        return List.of(500, 9000, 10000, 15000);
    }
}
