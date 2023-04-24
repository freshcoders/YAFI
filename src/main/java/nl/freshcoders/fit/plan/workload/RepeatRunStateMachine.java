package nl.freshcoders.fit.plan.workload;

import nl.freshcoders.fit.Orchestrator;
import nl.freshcoders.fit.plan.workload.states.*;

public class RepeatRunStateMachine implements ExecutionMachine {
    private WorkloadState currentState;

    Orchestrator context;

    @Override
    public void setContext(Orchestrator context) {
        this.context = context;
    }

    public RepeatRunStateMachine() {
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
        }else if (currentState instanceof WaitForReferenceExecutionState) {
            currentState = new ExtractRunExperimentState();
        }else if (currentState instanceof ExtractRunExperimentState) {
            currentState = new WaitForPerturbedExecutionState();
        } else if (currentState instanceof WaitForPerturbedExecutionState) {
            currentState = new AnalysePerturbedExecutionState();
        } else if (currentState instanceof AnalysePerturbedExecutionState) {
            currentState = new WaitForReferenceExecutionState();
        }
        System.out.println("now in: " + currentState.getClass().getSimpleName());
    }
}
