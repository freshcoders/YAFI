package nl.freshcoders.fit;


import nl.freshcoders.fit.node.EventSimulator;
import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.event.EventQueue;
import nl.freshcoders.fit.plan.parser.PlanParser;
import nl.freshcoders.fit.plan.runner.FailurePlanRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *
 */
public class Orchestrator {

    private static Orchestrator instance;

    FailurePlanRunner failurePlanRunner;

    List<Runnable> runnables = new ArrayList<>();

    Map<String, Thread> runnableThreads = new HashMap<>();

    /**
     * We allow one instance, because we connect to agents on a limited connection.
     */
    private Orchestrator() {
        String filePath = "C:\\Users\\nickd\\Documents\\Workspace\\YAFI\\src\\main\\resources\\example\\failure_plan.yml";
        FailurePlan fp = PlanParser.fromYamlFile(filePath);
        EventQueue eq = new EventQueue();
        failurePlanRunner = new FailurePlanRunner(fp, eq);
    }

    /**
     * Main loop of the orchestrator, where we handle events and send actions.
     * This call is blocking!
     */
    public void run() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        while (true) {
            if (executor.getActiveCount() >= 1) {
                continue;
            }
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
            executor.execute(failurePlanRunner);
        }
    }

    public static Orchestrator getInstance() {
        if (instance == null) {
            instance = new Orchestrator();
        }
        return instance;
    }

    public void addEventSimulator() {
        EventSimulator eventSim = new EventSimulator(
                failurePlanRunner
        );
        Thread thread = new Thread(eventSim);
        thread.start();
        // not necessary?
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        runnables.add(eventSim);
        runnableThreads.put(eventSim.getClass().getSimpleName(), thread);
    }



}
