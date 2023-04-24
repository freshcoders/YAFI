package nl.freshcoders.fit.plan.workload.states;

import nl.freshcoders.fit.Orchestrator;

public interface WorkloadState {
    boolean isReady();
    void execute(Orchestrator orchestrator);
}
