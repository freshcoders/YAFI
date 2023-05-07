import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.parser.PlanParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

public class TestFaultInjector {

    @Test
    public void testValidPlan() {
        ClassLoader classLoader = getClass().getClassLoader();

        FailurePlan validPlan = PlanParser.fromYamlFile(classLoader.getResource("valid_plan.yml").getFile());

        Assertions.assertEquals(1, validPlan.getEvents().size());

        // We assume there is only clock events and there is at least one, events are stored by their trigger type
        ConditionalEvent delay = validPlan.getEvents().get("clock").get(0);

        Assertions.assertEquals(1, validPlan.getEvents().size());
        // Since the fault was retrieved from the "events" map as a delay, this should be true
        Assertions.assertEquals("clock", delay.getType());
    }

    @Test
    public void testInvalidPlan() {
        ClassLoader classLoader = getClass().getClassLoader();

        Assertions.assertThrows(
                NullPointerException.class,
                () ->
                        PlanParser.fromYamlFile(classLoader.getResource("invalid_plan.yml").getFile())
        );
    }

}
