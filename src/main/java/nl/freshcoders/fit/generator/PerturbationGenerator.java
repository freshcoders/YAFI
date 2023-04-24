package nl.freshcoders.fit.generator;

import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.parser.PlanParser;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;

public class PerturbationGenerator {

    private List<Generator> generators = new LinkedList<>();

    int combinations = 0;

    public void registerGenerator(Generator g) {
        generators.add(g);
    }

    public Queue<FailurePlan> accumulatePlans() {
        Queue<FailurePlan> plans = new LinkedList<>();

        for (Generator generator : generators) {
            plans.addAll(generator.getGenerations());
        }
        plans.addAll(mergePlans(plans));
//        dedupe(plans);
        return plans;
    }
//
//    private void dedupe(Queue<FailurePlan> plans) {
////        Queue<FailurePlan> uniquePlans = new LinkedList<>();
////        for (FailurePlan plan : plans) {
////            plan.getEvents().
////        }
//        return plans;
//    }

    public Queue<FailurePlan> mergePlans(Queue<FailurePlan> plans) {
        Queue<FailurePlan> planCopy = new LinkedList<>(plans);
        Queue<FailurePlan> newPlans = new LinkedList<>();
        for (int i = 0; i < planCopy.size() - 1; i++) {
            FailurePlan p1 = planCopy.poll();
            int remainingSize = planCopy.size();
            for (int j = 0; j < remainingSize; j++) {
                FailurePlan p2 = planCopy.peek();
                boolean classConflict = checkForClassConflict(p1, p2);
                if (p1.isMergeable() && p2.isMergeable() && classConflict)
                    newPlans.add(mergeTwo(p1, p2));
            }
        }
        return newPlans;
    }

    private boolean checkForClassConflict(FailurePlan p1, FailurePlan p2) {
        Set<String> classnames = new HashSet<>();

        p1.getEvents().values().stream().forEach(
                elem -> {
                    elem.forEach(event ->
                            classnames.add(event.getFault().getOccurrence().getLocation().getClassName()));
                }
        );
        p2.getEvents().values().stream().forEach(
                elem -> {
                    elem.forEach(event ->
                            classnames.add(event.getFault().getOccurrence().getLocation().getClassName()));
                }
        );

        long count1 = p1.getEvents().values().stream().flatMap(Collection::stream).collect(Collectors.toList()).stream().count();
        long count2 = p2.getEvents().values().stream().flatMap(Collection::stream).collect(Collectors.toList()).stream().count();

        return classnames.size() == (count1 + count2);
    }

    public FailurePlan mergeTwo(FailurePlan fp1, FailurePlan fp2) {
        fp1.dumpPlan();
        FailurePlan newFp = PlanParser.fromInputStream(new ByteArrayInputStream(fp1.getRawPlan().getBytes()));

        fp2.getEvents().values().stream()
                .flatMap(Collection::stream)
                .forEach(ce -> newFp.addEvent(ce));
        newFp.reference = "plan-combinations-" + ++combinations;
        newFp.dumpPlanToFile(newFp.reference + ".yml");
        return newFp;
    }
}
