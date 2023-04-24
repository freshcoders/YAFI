package nl.freshcoders.fit.generator;

import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.FailurePlanBuilder;
import nl.freshcoders.fit.plan.event.fault.Occurrence;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static nl.freshcoders.fit.helper.FailurePlanHelper.*;

public abstract class TargetedGenerator implements Generator {

    public FailurePlan currentPlan = null;

    public Queue<FailurePlan> generations = new LinkedList<>();

    public Integer generationNumber = 0;

    protected List<String> planSignatures = new ArrayList<>();

    protected void generate(String type, String clazz, String method, String config, int triggerTime, String host) {
        generate(type, clazz, method, config, triggerTime, host, false);
    }

    protected void generate(String type, String clazz, String method, String config, int triggerTime, String host, boolean mergeable) {

        FailurePlan plan = new FailurePlanBuilder().withHosts(currentPlan.getHosts()).build();

        Occurrence occurrence = buildOnceOccurrence(clazz, method);
        if (host != null) {
            occurrence.setTarget(host);
            // TODO: validate host with currentplan
            boolean hostInConfig = currentPlan.getHosts().values().stream().anyMatch(
                    o -> o.getTargetName().contains(host)
            );

            if (host.equals("all"))
                occurrence.setTarget("all");
            else if (!hostInConfig) {
                return;
            }
        }
        plan.addEvent(buildEvent(
                buildFault(type, config, occurrence),
                buildClockTrigger(triggerTime, 0)
        ));
        // XXX: move this logic to the accumulator? we'd have to renumber..
        if (planSignatures.contains(plan.getSignature())) {
            return;
        }
        planSignatures.add(plan.getSignature());
        generationNumber++;
        String planName = "plan-" + type + "-" + generationNumber;
        plan.dumpPlanToFile(planName + ".yml");
        plan.reference = planName;
        generations.add(plan);
        plan.setMergeable(mergeable);
    }

    public Queue<FailurePlan> getGenerations() {
        return generations;
    }

}
