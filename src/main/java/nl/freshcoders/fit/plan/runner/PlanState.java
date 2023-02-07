package nl.freshcoders.fit.plan.runner;

import nl.freshcoders.fit.node.NodeController;
import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.event.fault.Fault;
import nl.freshcoders.fit.plan.event.trigger.Trigger;

import java.util.*;


/**
 * Stores the state of the injection plan, such as when and where
 * injections were done and which triggers have executed.
 */
public class PlanState {

    private Map<String, String> injectionStates;

    private Map<String, String> triggerStates;

    private List<NodeController> targetList;
    private Random rand = new Random();

    PlanState(List<NodeController> targetList) {
        injectionStates = new HashMap<>();
        triggerStates = new HashMap<>();
        this.targetList = targetList;
    }

    public void logInjection(Fault f, String host) {
        injectionStates.put(f.toString(), host);

    }

    public void logTrigger(Trigger trigger, String host) {
        triggerStates.put(trigger.toString(), host);
    }

    /**
     * If the target is any, the actual hostname needs to be chosen.
     * Warning: does not verify if the host is still active or valid!
     * @return The hostname of the injection target.
     */
    public List<String> getInjectionTarget(ConditionalEvent ce) {
        List<String> targets = new ArrayList<>();
        switch (ce.getFault().getOccurrence().getTarget()) {
            case "any":
                // pick random from target list, or persist previous
                String anyTarget = injectionStates.getOrDefault(
                        ce.getFault().toString(),
                        targetList.get(rand.nextInt(targetList.size())).target.getTargetName());
                targets.add(anyTarget);
                break;
            case "match_trigger":
                // get the fault target from the triggering node
                String mtTarget = triggerStates.get(ce.getTrigger().toString());
                if (mtTarget == null) {
                    throw new RuntimeException("match_trigger can only be used when a trigger precedes the fault! No trigger found when injecting fault!");
                }
                targets.add(mtTarget);
                break;
        }
        return targets;
    }

    /**
     * Possible class to store encapsulation data, such as timestamp for events.
     * Might also add some expire time in here if the conditions are valuable.
     */
    public class Encapsulation {

    }
}
