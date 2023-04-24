package nl.freshcoders.fit.plan;

import nl.freshcoders.fit.Orchestrator;
import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.log.LogAction;
import nl.freshcoders.fit.plan.parser.TraceInstaller;
import nl.freshcoders.fit.target.LocalTarget;
import nl.freshcoders.fit.target.RemoteTarget;
import nl.freshcoders.fit.target.Target;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FailurePlan {

    Map<String, List<ConditionalEvent>> events;

    Map<String, LogAction> log;

    Map<String, Target> hosts;

    String rawPlan = "";
    public String reference = "";
    private boolean mergeable;

    public FailurePlan() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public FailurePlan(Map<String, List<ConditionalEvent>> events,
                       Map<String, LogAction> log,
                       Map<String, Target> hosts) {
        this.events = events;
        this.log = log;
        this.hosts = hosts;
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

    public void setHosts(Map<String, Target> hosts) {
        this.hosts = hosts;
    }

    public void setWorkload() {

    }

    public Map<String, Target> getHosts() {
        return hosts;
    }

    public void addHost(String ip, Integer port) {
        hosts.put(ip + port.toString(), new RemoteTarget(ip, port));
    }

    public void addLocalHost(Integer port, String uid) {
        LocalTarget localTarget = new LocalTarget(port, uid);
        hosts.put(localTarget.getTargetName(), localTarget);
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
        for (Map.Entry<String, Target> hostEntry : hosts.entrySet()) {
            hostsList.add(
                    new HashMap<>() {{
                        put("ip", hostEntry.getValue().getHost());
                        put("port", hostEntry.getValue().getPort());
                        put("uid", hostEntry.getValue().getUid());
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

    public String getSignature() {
        Map<String, ?> mappedPlan = mapData();
        Integer signature = mappedPlan
                .values()
                .stream()
                .map(o -> o.hashCode())
                .reduce(0, (a, b) -> a + b);
        return signature.toString();
    }

    /**
     * Dump the plan configuration to the raw plan field, for exporting.
     * This is applicable when the plan was defined programmatically and
     * needs to be exported as yaml.
     **/
    public void dumpPlan() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        Yaml yaml = new Yaml(options);
        Map<String, Object> data = new HashMap<>();
        data.putAll(this.mapData());
        this.setRawPlan(yaml.dump(data));
    }


    /**
     * Dumps to file as well as to the raw plan field as `DumpPlan`
     * @param file
     * @return
     */
    public void dumpPlanToFile(String file) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        Yaml yaml = new Yaml(options);
        Map<String, Object> data = new HashMap<>();
        data.putAll(this.mapData());
        this.setRawPlan(yaml.dump(data));
        try {
            String generationPath = System.getProperty("user.dir") + "/" + "generatedplans/" + Orchestrator.getInstance().getRunId();
            Path path = Paths.get(generationPath);
            Files.createDirectories(path);
            FileWriter fw = new FileWriter(generationPath + "/" + file);
            yaml.dump(data, fw);
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setMergeable(boolean mergeable) {
        this.mergeable = mergeable;
    }

    public boolean isMergeable() {
        return mergeable;
    }
}
