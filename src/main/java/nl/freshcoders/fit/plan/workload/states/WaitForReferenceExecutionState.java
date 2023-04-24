package nl.freshcoders.fit.plan.workload.states;

import nl.freshcoders.fit.Orchestrator;
import nl.freshcoders.fit.connection.LocalConnection;

public class WaitForReferenceExecutionState implements WorkloadState {
    private boolean done = false;
    private boolean initializedWebapps = false;
    private int executions = 0;

    @Override
    public boolean isReady() {
        return done;
    }

    @Override
    public void execute(Orchestrator orchestrator) {
        // Since we currently only gather application data from outside
        // the application, we do not have to run the failure plan along
        // the reference run

        // This will execute the functional test, setting up the required webapps
        // We only want to run this once, then gather results
        // For this example, we set up the application beforehand, it could be part of the control script

        // The path to the control script, it should be located in the functional test directory, specified by the
        // user, unless it is available as `/usr/bin/sut-control.sh`. TODO: potentially add PATH support.
        String controlScript = System.getProperty("functional.dir", "/usr/bin/") + "sut-control.sh";

        orchestrator.getFailurePlanRunner().logUserAction("starting functional test");


        String result = LocalConnection.executeCommand("bash " + controlScript + " test 8081");
        String result2 = LocalConnection.executeCommand("bash " + controlScript + " test 8082");

        orchestrator.getFailurePlanRunner().logUserAction(result);
        orchestrator.getFailurePlanRunner().logUserAction(result2);
        orchestrator.getFailurePlanRunner().setInvariant("response", result);
        orchestrator.getFailurePlanRunner().setInvariant("response2", result2);

        done = true;
        orchestrator.getFailurePlanRunner().logUserAction("reference complete");
    }
}
