package nl.freshcoders.fit.generator;

import nl.freshcoders.fit.injector.FaultTypes;
import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.parser.PlanParser;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ClockPlanGenerator extends TargetedGenerator {

    public ClockPlanGenerator(FailurePlan plan) {
        this.currentPlan = plan;
    }

    /**
     * Create a delay list on the second scale.
     * @return
     */
    private List<Long> generateSkewList() {
        // We use fixed timeout values for now
        int timeout = 10 * 1000;
        return List.of(TimeUnit.HOURS.toMillis(1L));
    }

    public void generateSkew(String clazz, String method, Long delay, int triggerTime, String host) {
        generate("clock-skew", clazz, method, delay.toString(), triggerTime, host);
    }
    public void generateFixedClockPlan(String timestamp) {
        // use Long for timestamp to prevent injection error
        timestamp += "L";
        generate(
                FaultTypes.FIXED_CLOCK.toString(),
                "abstract java.time.Clock",
                "millis",
                timestamp, 0, "all");
        generate(
                FaultTypes.FIXED_CLOCK.toString(),
                "java.lang.System",
                "currentTimeMillis",
                timestamp, 0, "all");
        FailurePlan clock1 = generations.poll();
        FailurePlan clock2 = generations.poll();

        generations.add(mergeTwo(clock1, clock2));
    }

    public void generateSkews(String clazz, String method) {
        for (Long skew : generateSkewList()) {
            currentPlan.getHosts().values().forEach(
                    host ->
                            generateSkew(clazz, method, skew, 0, host.getTargetName())
            );
        }
    }

    public FailurePlan mergeTwo(FailurePlan fp1, FailurePlan fp2) {
        fp1.dumpPlan();
        FailurePlan newFp = PlanParser.fromInputStream(new ByteArrayInputStream(fp1.getRawPlan().getBytes()));

        fp2.getEvents().values().stream()
                .flatMap(Collection::stream)
                .forEach(ce -> newFp.addEvent(ce));
        newFp.reference = "plan-clock-" + ++generationNumber;
        newFp.dumpPlanToFile(newFp.reference + "-source.yml");
        return newFp;
    }
}
