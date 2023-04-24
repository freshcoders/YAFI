package nl.freshcoders.fit.target;

import nl.freshcoders.fit.environment.EnvironmentDetector;

public class LocalTarget extends Target {

    public LocalTarget(int port) {
        determineOs();
        this.port = port;
    }

    public int port;

    public LocalTarget(int port, String uid) {
        this(port);
        this.uid = uid;
    }

    /**
     * Note: this will not work for dockers, so local
     */
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
        return port;
    }

    @Override
    public String getTargetName() {
        return "LOCAL:" + port + ":" + uid;
    }

    @Override
    public boolean isLocal() {
        return port == 0;
    }

}
