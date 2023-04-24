package nl.freshcoders.fit.plan.workload;

import nl.freshcoders.fit.Orchestrator;

public interface ExecutionMachine extends Runnable {
    void execute();

    void setContext(Orchestrator orchestrator);

    @Override
    default void run() {
        execute();
    }
}
