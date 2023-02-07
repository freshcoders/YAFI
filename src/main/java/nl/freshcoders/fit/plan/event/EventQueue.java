package nl.freshcoders.fit.plan.event;

import nl.freshcoders.fit.plan.event.trigger.TriggerValue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventQueue {

    Queue<TriggerValue> queue = new ConcurrentLinkedQueue<>();

    public EventQueue() {
    }
    
    public void add(TriggerValue trigger) {
        queue.add(trigger);
    }

    public TriggerValue poll() {
        if (queue.isEmpty())
            return null;
        return queue.poll();
    }

}
