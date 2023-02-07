package nl.freshcoders.fit.plan.event.trigger;

import java.util.Map;

public class Trigger {

    private String type;

    private TriggerArguments args;

    public Trigger(String type, Map<String, String> arguments) {
        this(type, TriggerArgumentsFactory.get(type, arguments));
    }

    public Trigger(String type, TriggerArguments arguments) {
        this.type = type;
        this.args = arguments;
    }

    public String getType() {
        return type;
    }

    public TriggerArguments getArgs() {
        return args;
    }

    public boolean match(TriggerValue t) {
        // TODO: see if we have to regex, or do matching differently based on types
        // If we are going to validate args through type, we may as well override the compare/equals
        // for each type (DelayArgs, ExceptionArgs...)
        if (getArgs().match(t)) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Trigger{" +
                "type='" + type + '\'' +
                ", args=" + args +
                '}';
    }

    public Object mapData() {
        return Map.of(
                "type", getType(),
                "arguments", getArgs().mapData()
        );
    }
}
