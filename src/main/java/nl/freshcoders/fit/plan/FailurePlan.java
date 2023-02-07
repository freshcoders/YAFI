package nl.freshcoders.fit.plan;

import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.log.LogAction;
import nl.freshcoders.fit.plan.parser.TraceInstaller;
import nl.freshcoders.fit.plan.workload.WorkLoad;
import nl.freshcoders.fit.target.RemoteTarget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FailurePlan {

    WorkLoad workload;

    Map<String, List<ConditionalEvent>> events;

    Map<String, LogAction> log;

    Map<String, RemoteTarget> hosts;

    String rawPlan = "";

    public FailurePlan() {
        workload = new WorkLoad();
        events = new HashMap<>();
        log = new HashMap<>();
        hosts = new HashMap<>();
    }

    public void setRawPlan(String text) {
        rawPlan = text;
    }

    public String getRawPlan() {
        return rawPlan;
    }

    public void install() {
        for (ConditionalEvent event : getTraceEvents()) {
            TraceInstaller.parseBytemanTrigger(event);
        }
        // if we have a clock event, we want to install it once?? relative clock?

        for (ConditionalEvent event : getClockEvents()) {
            TraceInstaller.parseBytemanTrigger(event);
        }
    }

    public void addEvent(ConditionalEvent event) {
        String type = event.getType();
        List<ConditionalEvent> typedEvents = getEventsByType(type);
        typedEvents.add(event);
        events.put(type, typedEvents);
        // de-dupe events
        deduplicateEvents();
    }

    public void deduplicateEvents() {
        Map<String, String> eventHashes = new HashMap<>();
        for (Map.Entry<String, List<ConditionalEvent>> stringListEntry : events.entrySet()) {
            List<ConditionalEvent> uniqueCEs = new ArrayList<>();
            System.out.println("dedupe:" + stringListEntry.getKey());
            for (ConditionalEvent conditionalEvent : stringListEntry.getValue()) {
                if (eventHashes.get(conditionalEvent.toString()) == null) {
                    uniqueCEs.add(conditionalEvent);
                }
            }
            events.put(stringListEntry.getKey(), uniqueCEs);
        }
    }

    public void addLog() {

    }

    public void setWorkload() {

    }

    public Map<String, RemoteTarget> getHosts() {
        return hosts;
    }

    public void addHost(String ip, Integer port) {
        // Will probably become (host.getIp(), Host)..
        hosts.put(ip + port.toString(), new RemoteTarget(ip, port));
    }

    public Map<String, List<ConditionalEvent>> getEvents() {
        return events;
    }

    public List<ConditionalEvent> getEventsByType(String type) {
        return events.getOrDefault(type, new ArrayList<>());
    }

    public List<ConditionalEvent> getTraceEvents() {
        return events.getOrDefault("trace", new ArrayList<>());
    }
    public List<ConditionalEvent> getClockEvents() {
        return events.getOrDefault("clock", new ArrayList<>());
    }

    public Map<String,?> mapData() {
        Map<String, Object> mappedData = new HashMap<>();

        // HOSTS
        List<Map<String, Object>> hostsList = new ArrayList<>();
        for (Map.Entry<String, RemoteTarget> hostEntry : hosts.entrySet()) {
            hostsList.add(
                    new HashMap<>() {{
                        put("ip", hostEntry.getValue().getHost());
                        put("port", hostEntry.getValue().getPort());
                    }}
            );
        }
        mappedData.put("hosts", hostsList);


        // EVENTS
        List<Map<String, Object>> eventsList = new ArrayList<>();
        for (Map.Entry<String, List<ConditionalEvent>> event : events.entrySet()) {
            for (ConditionalEvent conditionalEvent : event.getValue()) {
                eventsList.add(Map.of(
                        "fault", conditionalEvent.getFault().mapData(),
                        "trigger", conditionalEvent.getTrigger().mapData()
                ));
            }
        }
        mappedData.put("events", eventsList);


        return mappedData;
    }
}
