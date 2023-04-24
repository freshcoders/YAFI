package nl.freshcoders.fit.plan.workload.states;

import nl.freshcoders.fit.Orchestrator;

public class DeadState implements WorkloadState {
    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void execute(Orchestrator orchestrator) {
        // pass
    }
}
