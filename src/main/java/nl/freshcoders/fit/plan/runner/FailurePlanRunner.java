package nl.freshcoders.fit.plan.runner;

import nl.freshcoders.fit.connection.AgentConnection;
import nl.freshcoders.fit.connection.Connection;
import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.node.NodeController;
import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.event.EventQueue;
import nl.freshcoders.fit.plan.event.trigger.TriggerValue;
import nl.freshcoders.fit.target.RemoteTarget;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.logging.Logger;

/**
 * Run a failure plan in combination with it's requirements, such as nodes we connect to, a queue with events to handle
 * and additional plugins (NYI).
 */
public class FailurePlanRunner implements Runnable {

    EventQueue eventQueue;
    FailurePlan failurePlan;
    Map<String, NodeController> nodeControllers = new HashMap<>();
    PlanState planState;
    Map<String, Integer> keepAliveTicker = new HashMap<>();

    public FailurePlanRunner(FailurePlan fp, EventQueue queue) {
        failurePlan = fp;
        eventQueue = queue;
        addNodeControllersFromHosts(fp.getHosts());
        planState = new PlanState(new ArrayList<>(nodeControllers.values()));
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        Logger.getLogger("FPR").info("Hosts initialized");

        // Does transfer work without initializing the connection first? Should it?
//        ConnectionPool.get(ConnectionPool.CONNECTION_TYPES.SOCKET, new RemoteTarget("localhost"));
        dumpPlan();
        transferPlanToAll();
    }

    private void dumpPlan() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        Yaml yaml = new Yaml(options);
        Map<String, Object> data = new HashMap<>();
        data.putAll(failurePlan.mapData());
        this.failurePlan.setRawPlan(yaml.dump(data));
    }

    /**
     * Crude method to transfer a plan to an agent. This should be replace by another file transfer mechanism.
     */
    private void transferPlanToAll() {
        String rawPlan = failurePlan.getRawPlan();
        System.out.println("transferring");

        nodeControllers.values().forEach(nc -> {
            System.out.println("to:" + nc.target.getTargetName());

            Connection conn = ConnectionPool.get(ConnectionPool.CONNECTION_TYPES.SOCKET, nc.target);
            conn.run("file:transfer:start:plan.yml");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String[] rawPlanLines = rawPlan.split("\n");
            for (String line : rawPlanLines) {
                conn.run(line);

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            conn.run("file:transfer:end:plan.yml");
        });
    }

    private void addNodeControllersFromHosts(Map<String, RemoteTarget> hosts) {
        for (RemoteTarget host : hosts.values()) {
            NodeController node = new NodeController(host);
            addNodeController(node);
        }
    }


    public void addNodeController(NodeController nc) {
        nodeControllers.put(nc.target.getTargetName(), nc);
    }

    @Override
    public void run() {
        // First, we receive, to see if we have unhandled events
        for (NodeController nodeController : nodeControllers.values()) {
            AgentConnection agentConnection = ConnectionPool.getAgentConnection(nodeController.target);
            String node = nodeController.target.getTargetName();

            Integer nodeTick = keepAliveTicker.getOrDefault(node, 0);
            if (nodeTick > 500) {
                keepAliveTicker.put(node, 0);
                agentConnection.run("ping");
            } else {
                keepAliveTicker.put(node, ++nodeTick);
            }
            if (!agentConnection.isOpen()) continue;
            // Should we receive here?
            String message = agentConnection.receive();
            if (message == null) {
                continue;
            }
            Logger logger = Logger.getLogger("FPR");
            logger.info("RECV: " + message);

            // if the message we receive is of type "event", we add it to our event queue
            // TODO: optional: create Message objects (or MessageParserUtil), so logic is easier to follow here
            if (message.startsWith("event:")) {
                String information = message.substring(6);
                String[] parts = information.split(":");
                String eventType = parts[0];
                String eventValue = parts[1];
                TriggerValue triggerValue = new TriggerValue(nodeController.target.getTargetName(), eventType, eventValue);
                appendQueue(triggerValue);
            }

            // TODO: here, we could handle some rule-condition events
            // For example, for ensuring no 2 events take place within x ms
            // of each other, we have to wait x ms here to see if we encounter
            // such a situation (only if this event is supposed to originate
            // from different nodes. If it may come from both different nodes
            // AND the node ourselves, we should run the scenario in two modes
            // one, blocking and one non-blocking to ensure this. However, this
            // is unlikely to be implemented for now..
            // We do this like:
            // check if this is a blocking call, by seeing if the message
            // contains an id (prefix [0-9]*), probably make this a fixed-length
            // string of the integer maybe 7, so we support 1 million events.
            // splitting on index will be faster than regex.. (could be a configurable)
            // or we rotate on 10000-ish
            // if it isnt, just log the traceValue in the trace history and
            // continue. otherwise:
//            ConditionalEvent ce = failurePlan.getMatch(message)
            // TODO: put it in a while..
//            if (ce.getTrigger.timeBetween(previousevent???, ) < TIME_BETWEEN_EVENTS..
            // if the time has passed, break
            // if we encounter it, return a fault id? since all faults
            // should be installed on the target through the fault-plan
            // we can send the hash code like "XXX:YYY",
            // with XXX = prefix id and YYY = event hash
//            System.out.println("message=" + message);
        }
        
        // We read the event queue to handle the triggered events
        // XXX: should be thread safe..
        TriggerValue t = eventQueue.poll();
        if (t == null) {
            return;
        }
//        System.out.println("found event: " + t.type() + "->" + t.get("traceClass"));
        List<ConditionalEvent> events = failurePlan.getEventsByType(t.type());
//        System.out.println("checking...");
        for (ConditionalEvent ce : events) {
//            System.out.println(ce);
//            System.out.println(ce.getFault());
//            System.out.println(ce.getTrigger());
            if (ce != null && ce.getFault() != null && !ce.isTriggered() && ce.getTrigger().match(t)) {
                planState.logTrigger(ce.getTrigger(), t.getSource());
                // Triggering fault!
//                System.out.println("fault...");
                // set fault to triggered, TODO: add the source to a list so we know how to deal with these
                ce.trigger();
                List<String> targetHosts = planState.getInjectionTarget(ce);
                for (String targetHost : targetHosts) {
//                    System.out.println(targetHost);
                    NodeController nc = nodeControllers.get(targetHost);
                    // TODO: check if the host exists and has an active connection
                    System.out.println("inj on " + nc.target.getTargetName());
                    nc.inject(ce.toHash(), ce.getFault().getType(), ce.getFault().getConfig());
                }
            }
        }
    }

    public void refresh() {
        for (NodeController nc : nodeControllers.values()) {
            ConnectionPool.get(ConnectionPool.CONNECTION_TYPES.SOCKET, nc.target);
        }
        for (ConditionalEvent trace : failurePlan.getEventsByType("trace")) {
            trace.reset();
        }
    }

    public void appendQueue(TriggerValue t) {
        eventQueue.add(t);
    }
}
