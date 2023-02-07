package nl.freshcoders.fit.example;

import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.event.fault.Fault;
import nl.freshcoders.fit.plan.event.fault.Occurrence;
import nl.freshcoders.fit.plan.event.trigger.ClockArguments;
import nl.freshcoders.fit.plan.event.trigger.Trigger;

public class ExampleEvent {

    public FailurePlan fp;

    public ExampleEvent() {
        fp = new FailurePlan();
        ConditionalEvent ce = new ConditionalEvent();
        Fault f1 = new Fault(
                "exception",
                "Main exception!",
                Occurrence.onceOccurrence());
        Trigger t1 = new Trigger("clock", new ClockArguments(100, 0));

        ce.setFault(f1);
        ce.setTrigger(t1);

        fp.addEvent(ce);
    }

}
