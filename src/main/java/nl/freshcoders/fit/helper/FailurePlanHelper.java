package nl.freshcoders.fit.helper;

import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.event.fault.Fault;
import nl.freshcoders.fit.plan.event.fault.Occurrence;
import nl.freshcoders.fit.plan.event.trigger.ClockArguments;
import nl.freshcoders.fit.plan.event.trigger.TraceArguments;
import nl.freshcoders.fit.plan.event.trigger.Trigger;

public class FailurePlanHelper {
    public static ConditionalEvent buildEvent(Fault f, Trigger t) {
        ConditionalEvent ce = new ConditionalEvent();

        ce.setTrigger(t);
        ce.setFault(f);

        return ce;
    }

    public static Fault buildDelayFault(String delay, Occurrence occurrence) {
        return buildFault("delay", delay, occurrence);
    }

    public static Fault buildFault(String name, String config, Occurrence occurrence) {
        return new Fault(
                name,
                config,
                occurrence
        );
    }

    public static Occurrence buildOnceOccurrence(String clazz, String method) {
        Occurrence occurrence = Occurrence.onceOccurrence();
        occurrence.setLocation(occurrence.new Location(clazz, method));
        return occurrence;
    }

    public static Trigger buildClockTrigger(int time, int duration) {
        return new Trigger("clock", new ClockArguments(time, duration));
    }

    public static Trigger buildTraceTrigger(String clazz, String method) {
        return new Trigger("trace", new TraceArguments(clazz, method));
    }

}
