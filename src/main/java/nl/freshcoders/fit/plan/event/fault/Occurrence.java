package nl.freshcoders.fit.plan.event.fault;

import java.util.Map;

public class Occurrence {
    private String timing;
    private Location location;
    private String target;

    public Occurrence(String timing, String target, Map<String, String> location) {
        this.timing = timing;
        this.target = target;

        if (location != null) {
            this.location = new Location(location.get("class"), location.get("method"));
        }
    }

    public static Occurrence onceOccurrence() {
        return new Occurrence("once", "any", null);
    }

    public static Occurrence persistentOccurrence() {
        return new Occurrence("persistent", "any", null);
    }

    public String getTiming() {
        return timing;
    }

    public void setTiming(String timing) {
        this.timing = timing;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Object mapData() {
        return Map.of(
                "timing", getTiming(),
                "location", getLocation().mapData(),
                "target", getTarget()
        );
    }

    public class Location {
        private String className;
        private String method;

        public Location(String className, String method) {
            this.className = className;
            this.method = method;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        @Override
        public String toString() {
            return "Location{" +
                    "className='" + className + '\'' +
                    ", method='" + method + '\'' +
                    '}';
        }

        public Object mapData() {
            return Map.of(
                    "class", className,
                    "method", getMethod()
            );
        }
    }

    @Override
    public String toString() {
        return "Occurrence{" +
                "timing='" + timing + '\'' +
                ", location=" + location +
                ", target='" + target + '\'' +
                '}';
    }
}
