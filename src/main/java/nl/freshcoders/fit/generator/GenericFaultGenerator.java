package nl.freshcoders.fit.generator;

import nl.freshcoders.fit.plan.FailurePlan;

public class GenericFaultGenerator extends TargetedGenerator {

    public GenericFaultGenerator(FailurePlan plan) {
        this.currentPlan = plan;
    }

    protected void generate(String type, String clazz, String method, String config, int triggerTime, String host) {
        super.generate(type, clazz, method, config, triggerTime, host);
    }

    public void generateInstantFaultAllHosts(String type, String clazz, String method, String config) {
        generate(type, clazz, method, config, 0, "all");
    }
}
