package nl.freshcoders.fit.plan.workload;

import nl.freshcoders.fit.Orchestrator;
import nl.freshcoders.fit.plan.workload.states.*;

public class WorkloadStateMachine implements ExecutionMachine {
    private WorkloadState currentState;

    Orchestrator context;

    @Override
    public void setContext(Orchestrator context) {
        this.context = context;
    }

    public WorkloadStateMachine() {
        currentState = new StartupState();
    }

    public void execute() {
        Thread t = new Thread(
                () -> {
                    while (true) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        // prevent adding logs while not running, causing a comodification error on writeout
                        if (context.getFailurePlanRunner().isRunning())
                            context.getFailurePlanRunner().getPlanState().logCpu();
                    }
                }
        );
        t.start();

        while (true) {
            currentState.execute(context);
            if (currentState.isReady()) {
                transitionToNextState();
            }
        }
    }

    private void transitionToNextState() {
        System.out.println("switching state..");
        if (currentState instanceof StartupState) {
            currentState = new WaitForReferenceExecutionState();
        } else if (currentState instanceof WaitForReferenceExecutionState) {
            currentState = new AnalyseReferenceExecutionState();
        } else if (currentState instanceof AnalyseReferenceExecutionState) {
            currentState = new WaitForPerturbedExecutionState();
        } else if (currentState instanceof WaitForPerturbedExecutionState) {
            currentState = new AnalysePerturbedExecutionState();
        } else if (currentState instanceof AnalysePerturbedExecutionState) {
            currentState = new DeadState();
        }
        System.out.println("now in: " + currentState.getClass().getSimpleName());
    }
}
