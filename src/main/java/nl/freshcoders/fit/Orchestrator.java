package nl.freshcoders.fit;


import nl.freshcoders.fit.node.EventSimulator;
import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.event.EventQueue;
import nl.freshcoders.fit.plan.parser.PlanParser;
import nl.freshcoders.fit.plan.runner.FailurePlanRunner;
import nl.freshcoders.fit.plan.workload.ExecutionMachine;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 */
public class Orchestrator {

    private static Orchestrator instance;
    public Queue<FailurePlan> generations;
    private FailurePlanRunner failurePlanRunner;
    private List<Runnable> runnables = new ArrayList<>();
    private Map<String, Thread> runnableThreads = new HashMap<>();
    private ExecutionMachine stateMachine;
    private String runId;

    /**
     * We allow one instance, because we connect to agents on a limited connection.
     */
    private Orchestrator() {
        String filePath = System.getProperty("user.dir") + "/src/main/resources/example/failure_plan.yml";
        FailurePlan fp = PlanParser.fromYamlFile(filePath);
        EventQueue eq = new EventQueue();
        this.generations = new LinkedList<>();

        failurePlanRunner = new FailurePlanRunner(fp, eq);
        long timestampInSeconds = System.currentTimeMillis() / 1000;
        Date date = new Date(timestampInSeconds * 1000);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd_HH-mm-ss");
        this.runId = String.format("run_%s", dateFormat.format(date));
    }

    public static Orchestrator getInstance() {
        if (instance == null) {
            instance = new Orchestrator();
        }
        return instance;
    }

    /**
     * Main loop of the orchestrator, where we handle events and send actions.
     * This call is blocking!
     */
    public void run() {
        if (stateMachine == null) {
            Logger.getLogger("Orchestrator").warning("No workflow indicated; terminating run");
            return;
        }
        Thread statemachine = new Thread(stateMachine);
        statemachine.start();
        while (true) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // Run the event simulator alongside normal operation to
            // add event trace values to the queues and help with
            // reconnection.
            for (Runnable runnable : runnables) {
                Thread thread = runnableThreads.get(runnable.getClass().getSimpleName());
                if (thread == null || !thread.isAlive()) {
                    thread = new Thread(runnable);
                    thread.start();
                    runnableThreads.put(runnable.getClass().getSimpleName(), thread);
                }
            }
        }
    }

    public ExecutionMachine getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(ExecutionMachine stateMachine) {
        this.stateMachine = stateMachine;
        stateMachine.setContext(this);
    }

    public void nextPlan() {
        failurePlanRunner.setRunning(false);
        failurePlanRunner.writeOut();
        failurePlanRunner.getExecutedPlanStates().add(failurePlanRunner.getPlanState());
        FailurePlan fp = generations.poll();
        EventQueue eq = new EventQueue();
        if (fp == null) {
            failurePlanRunner = new FailurePlanRunner(
                    new FailurePlan(),
                    eq, failurePlanRunner.getInvariants(),
                    failurePlanRunner.getExecutedPlanStates());
            return;
        }
        failurePlanRunner = new FailurePlanRunner(fp, eq, failurePlanRunner.getInvariants(), failurePlanRunner.getExecutedPlanStates());
    }

    public void addEventSimulator() {
        EventSimulator eventSim = new EventSimulator(
                failurePlanRunner
        );
        Thread thread = new Thread(eventSim);
        thread.start();

        runnables.add(eventSim);
        runnableThreads.put(eventSim.getClass().getSimpleName(), thread);
    }

    public FailurePlanRunner getFailurePlanRunner() {
        return failurePlanRunner;
    }

    public String getRunId() {
        return this.runId;
    }

    public void validateInvariant(String name, String value) {
        boolean valid = getFailurePlanRunner().validateInvariant(name, value);
        String expected = getFailurePlanRunner().getInvariant(name);
        if (!valid) {
            getFailurePlanRunner().logUserAction("Potential error found property: \"" + name + "\" did not match reference run!\n" +
                    "Expected: \n" + expected +
                    "Actual: \n" + value
            );
        }
    }

    public void clear() {
        failurePlanRunner.clear();
    }
}
