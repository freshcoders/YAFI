package nl.freshcoders.fit.plan.event.trigger;

public interface TriggerArguments {
    public boolean match(TriggerValue o);

    Object mapData();
}
