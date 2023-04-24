package nl.freshcoders.fit.plan.runner;

import nl.freshcoders.fit.Orchestrator;
import nl.freshcoders.fit.connection.AgentConnection;
import nl.freshcoders.fit.connection.Connection;
import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.environment.Environment;
import nl.freshcoders.fit.environment.EnvironmentDetector;
import nl.freshcoders.fit.node.NodeController;
import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.event.EventQueue;
import nl.freshcoders.fit.plan.event.trigger.TriggerValue;
import nl.freshcoders.fit.target.Target;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Run a failure plan in combination with it's requirements, such as nodes we
 * connect to, a queue with events to handle
 * and additional plugins (NYI).
 */
public class FailurePlanRunner implements Runnable {

    EventQueue eventQueue;
    FailurePlan failurePlan;
    Map<String, NodeController> nodeControllers = new HashMap<>();
    private Map<String, String> invariants = new HashMap<>();
    PlanState planState;
    List<PlanState> executedPlanStates;
    Map<String, Integer> keepAliveTicker = new HashMap<>();
    private boolean runnerInitialized;
    private boolean running = true;

    private Logger logger = Logger.getLogger(FailurePlanRunner.class);

    public FailurePlanRunner(FailurePlan fp, EventQueue queue) {
        failurePlan = fp;
        eventQueue = queue;
        addNodeControllersFromHosts(fp.getHosts());
        planState = new PlanState(new ArrayList<>(nodeControllers.values()));
        planState.setPlanReference(fp.reference);
        // try {
        // Thread.sleep(1000);
        // } catch (InterruptedException e) {
        // throw new RuntimeException(e);
        // }
        planState.logSystemAction("Plan initialized");
        runnerInitialized = true;
        executedPlanStates = new ArrayList<>();
        // Does transfer work without initializing the connection first? Should it?
        // ConnectionPool.get(ConnectionPool.CONNECTION_TYPES.SOCKET, new
        // RemoteTarget("localhost"));
    }

    public FailurePlanRunner(FailurePlan fp, EventQueue queue, List<PlanState> previousPlanState) {
        this(fp, queue);
        executedPlanStates.addAll(previousPlanState);
    }

    public FailurePlanRunner(FailurePlan fp, EventQueue queue, Map<String, String> invariants) {
        this(fp, queue);
        this.invariants = invariants;
    }

    public FailurePlanRunner(FailurePlan fp, EventQueue queue, Map<String, String> invariants,
            List<PlanState> previousPlanState) {
        this(fp, queue);
        this.invariants = invariants;
        executedPlanStates.addAll(previousPlanState);
    }

    public void logUserAction(String action) {
        planState.logAction(action);
    }

    /**
     * Crude method to transfer a plan to an agent. This should be replace by
     * another file transfer mechanism.
     */
    private void transferPlanToAll() {
        nodeControllers.values().forEach(nc -> {
            AgentConnection conn = ConnectionPool.getAgentConnection(nc.target);
            if (!nc.validateConnectionsForPlan(failurePlan))
                conn = ConnectionPool.getAgentConnection(nc.target);
            transferPlanToNode(nc, conn);
        });
    }

    private void transferPlanToNode(NodeController nc, Connection conn) {
        String rawPlan = failurePlan.getRawPlan();
        boolean startOk = conn.run("file:transfer:start:plan.yml");
        if (!startOk) {
            logger.warn("Could not start transfer for plan, aborting. Consider the result of this plan void: "
                    + failurePlan.reference);
        }
        try {
            Thread.sleep(15);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String[] rawPlanLines = rawPlan.split("\n");
        for (String line : rawPlanLines) {
            conn.run(line);

            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        conn.run("file:transfer:end:plan.yml");
    }

    private void addNodeControllersFromHosts(Map<String, Target> hosts) {
        for (Target host : hosts.values()) {
            NodeController node = new NodeController(host);
            addNodeController(node);
        }
    }

    public void addNodeController(NodeController nc) {
        nodeControllers.put(nc.target.getTargetName(), nc);
    }

    public NodeController getNodeController(Target target) {
        return nodeControllers.getOrDefault(target.getTargetName(), null);
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            tick();
        }
    }

    public void tick() {
        // Integer tick = keepAliveTicker.getOrDefault("self", 0);
        // if (tick % 100 == 0) {
        // planState.logCpu();
        // }
        // keepAliveTicker.put("self", ++tick);

        for (NodeController nodeController : nodeControllers.values()) {
            AgentConnection agentConnection = ConnectionPool.getAgentConnection(nodeController.target);
            if (!agentConnection.isOpen())
                continue;
            String node = nodeController.target.getTargetName();

            Integer nodeTick = keepAliveTicker.getOrDefault(node, 0);
            if (nodeTick > 5000) {
                keepAliveTicker.put(node, 0);
                // agentConnection.run("ping");
            } else {
                keepAliveTicker.put(node, ++nodeTick);
            }
            if (!agentConnection.isOpen())
                continue;
            // Should we receive here?
            String message = agentConnection.receive();
            if (message == null) {
                continue;
            }

            if (message.equals("exit")) {
                // reconnect
                ConnectionPool.getAgentConnection(nodeController.target).close();
                continue;
            }
            // if the message we receive is of type "event", we add it to our event queue
            // TODO: optional: create Message objects (or MessageParserUtil), so logic is
            // easier to follow here
            if (message.startsWith("event:")) {
                String information = message.substring(6);
                String[] parts = information.split(":");
                String eventType = parts[0];
                String eventValue = parts[1];
                TriggerValue triggerValue = new TriggerValue(nodeController.target, eventType, eventValue);
                appendQueue(triggerValue);
                // clock events come in continuously, we may have to filter them
                // XXX: enable.
                // planState.logRemoteEvent(nodeController, "[" + eventType + "] " +
                // eventValue);
            }
            if (message.startsWith("log:")) {
                String information = message.substring(4);
                String[] parts = information.split(":", 3);
                String timestamp = parts[0];
                String eventType = parts[1];
                String eventValue = parts[2];
                planState.logRemoteEvent(nodeController.target, "[" + eventType + "] " + eventValue,
                        Long.valueOf(timestamp));
            }
        }

        // We read the event queue to handle the triggered events
        // XXX: should be thread safe..
        TriggerValue t = eventQueue.poll();
        if (t == null) {
            return;
        }
        // System.out.println("found event: " + t.type() + "->" + t.get("traceClass"));
        List<ConditionalEvent> events = failurePlan.getEventsByType(t.type());
        // System.out.println("checking...");
        for (ConditionalEvent ce : events) {
            // System.out.println(ce);
            // System.out.println(ce.getFault());
            // System.out.println(ce.getTrigger());
            if (ce != null && ce.getFault() != null && !ce.isTriggered() && ce.getTrigger().match(t)) {
                planState.logTrigger(ce.getTrigger(), t.getSource());
                // Triggering fault!
                // System.out.println("fault...");
                // set fault to triggered, TODO: add the source to a list so we know how to deal
                // with these
                ce.trigger();
                List<String> targetHosts = planState.getInjectionTarget(ce);
                String targets = targetHosts.toString();
                planState.logSystemAction("Identified targets for current fault: " + targets);
                if (targetHosts.isEmpty()) {
                    logger.warn(
                            "An event was triggered without any target host! FailurePlan: " + failurePlan.reference);
                }
                for (String targetHost : targetHosts) {
                    // System.out.println(targetHost);
                    NodeController nc = nodeControllers.get(targetHost);
                    // TODO: check if the host exists and has an active connection
                    // System.out.println("inj on " + nc.target.getTargetName());
                    planState.logInjection(ce.toHash().toString(), nc.target);
                    // send the command to inject the fault..
                    nc.inject(ce.toHash(), ce.getFault().getType(), ce.getFault().getConfig());
                }
            }
        }
    }

    public void setup() {
        failurePlan.dumpPlan();

        transferPlanToAll();
        for (NodeController nc : nodeControllers.values()) {
            ConnectionPool.get(ConnectionPool.CONNECTION_TYPES.SOCKET, nc.target);
            nc.initialize();
        }
        for (ConditionalEvent trace : failurePlan.getEvents().values().stream().flatMap(List::stream)
                .collect(Collectors.toList())) {
            trace.reset();
        }
    }

    public void appendQueue(TriggerValue t) {
        eventQueue.add(t);
    }

    public boolean verify() {
        return runnerInitialized;
    }

    public FailurePlan getFailurePlan() {
        return failurePlan;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    public void writeOut() {
        Collections.sort(planState.getEventLog(), Comparator.comparingLong((a) -> a.time.toEpochSecond()));
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String planRef = planState.getPlanReference();
        if (planRef == null) {
            planRef = planState.eventLog.stream()
                    .map(p -> p.planReference)
                    .filter(p -> !p.equals("reference_run"))
                    .filter(p -> !p.isEmpty())
                    .findFirst()
                    .orElse("unknown_run");
        }
        String logDir = System.getProperty("user.dir") + "/log/" + Orchestrator.getInstance().getRunId();

        Path path = Paths.get(logDir);
        try {
            Files.createDirectories(path);

            if (!EnvironmentDetector.getOperatingSystem().equals(Environment.OS.WINDOWS)) {
                 Path symlink = Path.of(path.getParent() + "/latest");
                 Files.deleteIfExists(symlink);
                 Files.createSymbolicLink(symlink, path);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String logFilename = System.getProperty("user.dir") + "/log/" + Orchestrator.getInstance().getRunId() + "/"
                + planRef + ".log";
        String metricLogFilename = System.getProperty("user.dir") + "/log/" + Orchestrator.getInstance().getRunId()
                + "/" + "metrics.log";
        try (FileWriter writer = new FileWriter(logFilename);
                FileWriter metricWriter = new FileWriter(metricLogFilename, true)) {

            for (final PlanState.EventData eventData : planState.getEventLog()) {
                String data = eventData.data;
                if (data == null) {
                    data = "null";
                }
                Target host = eventData.target;
                ZonedDateTime time = eventData.time;
                String type = eventData.type;
                String logLine;
                if (host != null)
                    logLine = String.format("[%s] [%s] %s: %s\n", formatter.format(time), host.getTargetName(), type,
                            data);
                else
                    logLine = String.format("[%s] %s: %s\n", formatter.format(time), type, data);

                if (!type.contains("metric"))
                    writer.write(logLine);
                else
                    metricWriter.write(logLine);
            }
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    public void setInvariant(String key, String expectedValue) {
        if (key != null)
            this.invariants.put(key, expectedValue);
    }

    public String getInvariant(String key) {
        return this.invariants.getOrDefault(key, "");
    }

    public List<PlanState> getExecutedPlanStates() {
        return executedPlanStates;
    }

    public PlanState getPlanState() {
        return planState;
    }

    public boolean validateInvariant(String key, String actual) {
        String invariants = this.invariants.getOrDefault(key, "");
        if (invariants == null)
            return actual == null;
        return invariants.equals(actual);
    }

    public void cyclePlanState() {
        planState.logSystemAction("plan ended");
        getExecutedPlanStates().add(planState);
        planState = new PlanState(this.nodeControllers.values().stream().collect(Collectors.toList()));
    }

    public Map<String, String> getInvariants() {
        return invariants;
    }

    public void clearFaults() {
        planState.getInjectionStates().forEach(eventData -> {
            if (getNodeController(eventData.target) == null) {
                return;
            }
            getNodeController(eventData.target).clearInjection(Integer.valueOf(eventData.data));
        });
    }

    public void connectSut() {
        nodeControllers.values().forEach(
                nc -> {
                    AgentConnection agentConnection = ConnectionPool.getAgentConnection(nc.target);
                    agentConnection.run("byteman:attach");
                });
    }

    public void clear() {
        executedPlanStates = new ArrayList<>();
    }
}
