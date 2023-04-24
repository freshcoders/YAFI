package nl.freshcoders.fit.injector;

public enum FaultTypes {

    FIXED_CLOCK(),
    ;

    @Override
    public String toString() {
        return name().replace("_", "-").toLowerCase();
    }
}
