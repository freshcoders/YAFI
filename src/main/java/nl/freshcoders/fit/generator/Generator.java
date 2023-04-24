package nl.freshcoders.fit.generator;

import nl.freshcoders.fit.plan.FailurePlan;

import java.util.Queue;

@FunctionalInterface
interface Generator {
    Queue<FailurePlan> getGenerations();
}
