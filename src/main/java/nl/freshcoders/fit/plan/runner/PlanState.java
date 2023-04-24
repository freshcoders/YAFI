package nl.freshcoders.fit.plan.runner;

import nl.freshcoders.fit.injector.Node.MetricMonitor;
import nl.freshcoders.fit.node.NodeController;
import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.event.trigger.Trigger;
import nl.freshcoders.fit.target.Target;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;


/**
 * Stores the state of the injection plan, such as when and where
 * injections were done and which triggers have executed.
 */
public class PlanState {

    private List<EventData> injectionStates;
    private String planReference = "reference_run";

    private Map<String, String> triggerStates;

    private List<NodeController> targetList;
    private Random rand;

    public List<EventData> eventLog = new ArrayList<>();
    private long startTime = Long.MAX_VALUE;
    private long endTime = Long.MIN_VALUE;

    PlanState(List<NodeController> targetList) {
        if (System.getProperty("seed") == null) {
            rand = new Random();
        } else {
            long seed = Long.parseLong(System.getProperty("seed"));
            rand = new Random(seed);
        }

        injectionStates = new ArrayList<>();
        triggerStates = new HashMap<>();
        this.targetList = targetList;
    }

    public void logInjection(String eventId, Target target) {
        EventData data = encapsulateData("fault", eventId, target);
        addToLog(data);
        injectionStates.add(data);
    }

    public List<EventData> getInjectionStates() {
        return injectionStates;
    }

    public void logRemoteEvent(Target target, String event, long timestamp) {
        ZonedDateTime zoneTimeStamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        EventData data = new EventData(planReference, zoneTimeStamp,"event", event, target);
        addToLog(data);
    }

    public void logAction(String info) {
        addToLog(encapsulateData("user-action", info));
    }

    public void logSystemAction(String info) {
        addToLog(encapsulateData("system", info));
    }

    public void logTrigger(Trigger trigger, Target target) {
        EventData data = encapsulateData("", trigger.toString(), target);
        addToLog(data);
        triggerStates.put(trigger.toString(), target.getUid());
    }

    private void addToLog(EventData data) {
        startTime = Math.min(startTime, data.time.toEpochSecond() * 1000);
        endTime = Math.max(endTime, data.time.toEpochSecond() * 1000);
        eventLog.add(data);
    }

    public void logCpu() {
        double cpu = MetricMonitor.sampleCpu();
        addToLog(encapsulateData("metric:cpu", String.valueOf(cpu)));
    }

    private EventData encapsulateData(String event, String data) {
        ZonedDateTime now = ZonedDateTime.now();
        return new EventData(planReference, now, event, data);
    }
    private EventData encapsulateData(String event, String data, Target target) {
        ZonedDateTime now = ZonedDateTime.now();
        return new EventData(planReference, now, event, data, target);
    }
    public String getPlanReference() {
        return planReference;
    }

    public void setPlanReference(String planReference) {
        if (planReference.isEmpty())
            return;
        this.planReference = planReference;
    }

    public List<EventData> getEventLog() {
        return eventLog;
    }

    /**
     * If the target is any, the actual hostname needs to be chosen.
     * Warning: does not verify if the host is still active or valid!
     *
     * @return The hostname of the injection target.
     */
    public List<String> getInjectionTarget(ConditionalEvent ce) {
        List<String> targets = new ArrayList<>();
        String target = ce.getFault().getOccurrence().getTarget();
        switch (target) {
            case "any":
                // pick random from target list, or persist previous
                String anyTarget = injectionStates
                        .stream()
                        .filter(eventData -> eventData.data == ce.getFault().toString())
                        .map(eventData -> eventData.target.getTargetName())
                        .findFirst()
                        .orElse(targetList.get(rand.nextInt(targetList.size())).target.getTargetName());
                targets.add(anyTarget);
                break;
            case "all":
                // add all available nodes as targets
                for (NodeController nodeController : targetList) {
                    targets.add(nodeController.target.getTargetName());
                }
                break;
            case "match_trigger":
                // get the fault target from the triggering node
                String mtTarget = triggerStates.get(ce.getTrigger().toString());
                if (mtTarget == null) {
                    throw new RuntimeException("match_trigger can only be used when a trigger precedes the fault! No trigger found when injecting fault!");
                }
                targets.add(mtTarget);
                break;
            default:
                // check if the target matches any host, if so, return it
                for (NodeController nodeController : targetList) {
                    if (nodeController.target.getTargetName().equals(target)
                            || nodeController.target.getTargetName().trim().endsWith(target)
                    ) {
                        targets.add(nodeController.target.getTargetName());
                    }
                }
        }
        return targets;
    }

    public long getEndTime() {
        return endTime;
    }
    public long getStartTime() {
        return startTime;
    }

    /**f
     * Possible class to store encapsulation data, such as timestamp for events.
     * Might also add some expire time in here if the conditions are valuable.
     */
    public class EventData {
        ZonedDateTime time;
        String type;
        Target target;
        String data;
        String planReference;

        public EventData(String planReference, ZonedDateTime time, String type, String data, Target target) {
            this(planReference, time, type, data);
            this.target = target;
        }
        public EventData(String planReference, ZonedDateTime time, String type, String data) {
            this.time = time;
            this.type = type;
            this.data = data;
            this.planReference = planReference;
        }
    }
}
