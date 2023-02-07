package nl.freshcoders.fit.target;

import nl.freshcoders.fit.environment.EnvironmentDetector;

public class LocalTarget extends Target {

    public LocalTarget() {
        determineOs();
    }

    @Override
    public void determineOs() {
        os = EnvironmentDetector.getOperatingSystem();
    }

    @Override
    public String getHost() {
        return "127.0.0.1";
    }

    @Override
    public Integer getPort() {
        return null;
    }

    @Override
    public String getTargetName() {
        return null;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

}
