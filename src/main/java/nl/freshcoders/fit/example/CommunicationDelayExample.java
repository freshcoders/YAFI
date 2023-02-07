package nl.freshcoders.fit.example;

import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.event.fault.Fault;
import nl.freshcoders.fit.plan.event.fault.Occurrence;
import nl.freshcoders.fit.plan.event.trigger.ClockArguments;
import nl.freshcoders.fit.plan.event.trigger.Trigger;

public class CommunicationDelayExample extends FailurePlan {


    public CommunicationDelayExample() {
        super();
        // config should be done here..
        addHost("localhost", 13013);

    }

    public void addDelayFault(String delay, String clazz, String method) {
        ConditionalEvent ce = new ConditionalEvent();

        Trigger t1 = new Trigger("clock", new ClockArguments(100, 0));

        ce.setTrigger(t1);

        Occurrence occurrence = Occurrence.onceOccurrence();
        occurrence.setLocation(occurrence.new Location(clazz, method));
        Fault f1 = new Fault(
                "delay",
                delay,
                occurrence
        );
        ce.setFault(f1);
        addEvent(ce);
    }
}
