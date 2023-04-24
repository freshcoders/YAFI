package nl.freshcoders.fit.plan;

import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.log.LogAction;
import nl.freshcoders.fit.target.Target;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FailurePlanBuilder {

    private Map<String, List<ConditionalEvent>> events = new HashMap<>();

    private Map<String, LogAction> log;

    private Map<String, Target> hosts;
    public FailurePlanBuilder withHosts(Map<String, Target> hosts) {
        this.hosts = hosts;
        return this;
    }

    public FailurePlan build() {
        FailurePlan plan = new FailurePlan(events, log, hosts);
        return plan;
    }

    public FailurePlanBuilder withEvents(Map<String, List<ConditionalEvent>> events) {
        this.events = events;
        return this;
    }
}
