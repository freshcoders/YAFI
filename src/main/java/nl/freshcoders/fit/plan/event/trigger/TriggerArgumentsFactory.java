package nl.freshcoders.fit.plan.event.trigger;

import java.util.Map;

public class TriggerArgumentsFactory {

    public static TriggerArguments get(String triggerType, Map<String, String> arguments) {
        switch (triggerType) {
            case "clock":
                return new ClockArguments(Integer.valueOf(arguments.get("time")), Integer.valueOf(arguments.get("duration")));
            case "trace":
                arguments.get("class");
                arguments.get("method");
                return new TraceArguments(arguments.get("class"), arguments.get("method"));
        }
        return null;
    }

}
