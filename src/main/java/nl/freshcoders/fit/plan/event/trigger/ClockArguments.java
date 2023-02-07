package nl.freshcoders.fit.plan.event.trigger;

import java.util.Map;

public class ClockArguments implements TriggerArguments {

    private Integer time;
    private Integer duration;

    public ClockArguments(Integer clockTime, Integer duration) {
        this.time = clockTime;
        this.duration = duration;
    }

    public Integer getTime() {
        return time;
    }
    public Integer getDuration() {
        return duration;
    }

    @Override
    public boolean match(TriggerValue o) {
        Integer clockTime = Integer.valueOf(o.get("time"));
        if (clockTime > time) {
            // we are past the triggerpoint, we should trigger
            if (duration.equals(0))
                return true;
            if (clockTime > time + duration) {
                // use to de-install?
                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    @Override
    public Object mapData() {
        return Map.of(
                "time", getTime().toString(),
                "duration", getDuration().toString()
        );
    }

    @Override
    public String toString() {
        return "ClockArguments{" +
                "time=" + time +
                ", duration=" + duration +
                '}';
    }
}
