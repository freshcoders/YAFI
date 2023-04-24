package nl.freshcoders.fit.plan.workload.states;

import nl.freshcoders.fit.Orchestrator;
import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.connection.LocalConnection;
import nl.freshcoders.fit.generator.GenericFaultGenerator;
import nl.freshcoders.fit.generator.PerturbationGenerator;
import nl.freshcoders.fit.plan.runner.FailurePlanRunner;
import org.apache.log4j.Logger;


public class WaitForPerturbedExecutionState implements WorkloadState {
    private boolean done;

    @Override
    public boolean isReady() {
        return done;
    }

    @Override
    public void execute(Orchestrator orchestrator) {
        // We loop through generated failure plans while they are available
        if (orchestrator.generations.peek() == null) {
            done = true;
            return;
        } else {
            orchestrator.nextPlan();
            FailurePlanRunner fpr = orchestrator.getFailurePlanRunner();
            Logger.getLogger("FaultExec").info(
                    "Running plan: " + orchestrator.getFailurePlanRunner().getFailurePlan().reference +
                            " on execution: " + orchestrator.getRunId());
        }

        orchestrator.getFailurePlanRunner().setup();

        orchestrator.getFailurePlanRunner().logUserAction("starting services..");
        String controlScript = System.getProperty("functional.dir", "/usr/bin/") + "sut-control.sh";

        orchestrator.getFailurePlanRunner().connectSut();

        orchestrator.getFailurePlanRunner().logUserAction("FPR starting");
        Thread t = new Thread(orchestrator.getFailurePlanRunner());
        t.start();

        orchestrator.getFailurePlanRunner().logUserAction("starting functional connection");
//        LocalConnection.executeCommandSilent("bash " + controlScript + " exec");

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        orchestrator.getFailurePlanRunner().logUserAction("starting functional test");

        orchestrator.getFailurePlanRunner().logUserAction("timer:start:res1");
        String result = LocalConnection.executeCommand("bash " + controlScript + " test 8081");
        orchestrator.getFailurePlanRunner().logUserAction("timer:stop:res1");
         orchestrator.getFailurePlanRunner().logUserAction("timer:start:res2");
        String result2 = LocalConnection.executeCommand("bash " + controlScript + " test 8082");
        orchestrator.getFailurePlanRunner().logUserAction("timer:stop:res2");


        orchestrator.getFailurePlanRunner().logUserAction(result);
        orchestrator.getFailurePlanRunner().logUserAction(result2);
        orchestrator.validateInvariant("response", result);
        orchestrator.validateInvariant("response2", result2);
        
        // Restore normal operation..
        orchestrator.getFailurePlanRunner().clearFaults();

        // close the injector, so we clear rules on target nodes
        Logger.getLogger("FaultExec").debug("Connections closing");
        ConnectionPool.closeAll();
        orchestrator.getFailurePlanRunner().setRunning(false);
        try {
            t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}