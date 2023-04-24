package nl.freshcoders.fit.plan.workload.states;

import nl.freshcoders.fit.Orchestrator;
import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.connection.LocalConnection;
import nl.freshcoders.fit.tracing.ElasticSource;
import nl.freshcoders.fit.tracing.TraceReader;

public class AnalysePerturbedExecutionState implements WorkloadState {
    private boolean analysisComplete = false;

    static int runCount = 0;

    @Override
    public boolean isReady() {
        return analysisComplete;
    }

    @Override
    public void execute(Orchestrator orchestrator) {
        String controlScript = System.getProperty("functional.dir", "/usr/bin/") + "sut-control.sh";

//        LocalConnection.executeCommandSilent("bash " + controlScript + " stop");
        ConnectionPool.closeAll();
        orchestrator.getFailurePlanRunner().writeOut();
        orchestrator.getFailurePlanRunner().cyclePlanState();

        try {
            Thread.sleep(60_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        orchestrator.getFailurePlanRunner().getPlanState().setPlanReference("analyse-perturb");
        orchestrator.getFailurePlanRunner().getPlanState().logSystemAction("section:start:analysis");

        ElasticSource elasticSource = new ElasticSource("localhost", 9200);
        TraceReader reader = new TraceReader(elasticSource);
        orchestrator.nextPlan();
        reader.dumpExecutionAnalytics(orchestrator.getFailurePlanRunner());

        System.exit(0);
    }
}
