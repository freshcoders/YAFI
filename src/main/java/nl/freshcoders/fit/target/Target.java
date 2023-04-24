package nl.freshcoders.fit.target;

import nl.freshcoders.fit.environment.Environment;

public abstract class Target {

    public Environment.OS os;

    public String uid;

    public Environment.OS getOs() {
        return os;
    }

    public abstract boolean isLocal();

    public abstract void determineOs();

    public abstract String getHost();

    public abstract Integer getPort();

    public abstract String getTargetName();

    public String getUid() {
        return uid;
    }
}
