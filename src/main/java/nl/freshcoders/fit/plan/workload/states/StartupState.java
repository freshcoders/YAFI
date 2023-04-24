package nl.freshcoders.fit.plan.workload.states;

import nl.freshcoders.fit.Orchestrator;

public class StartupState implements WorkloadState {
    private boolean initiated = false;
    @Override
    public boolean isReady() {
        return initiated;
    }
    @Override
    public void execute(Orchestrator orchestrator) {
        // connect to hosts
        // parse / set up plan
        if (orchestrator.getFailurePlanRunner().verify()) {
            initiated = true;
        }
    }
}
