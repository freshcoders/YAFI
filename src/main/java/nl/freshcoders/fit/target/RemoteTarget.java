package nl.freshcoders.fit.target;

import nl.freshcoders.fit.connection.AgentConnection;
import nl.freshcoders.fit.connection.Connection;
import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.environment.Environment;

public class RemoteTarget extends Target {
    public final String host;

    public final Integer port;

    public RemoteTarget(String ip, Integer port) {
        this.host = ip;
        this.port = port;
    }

    public RemoteTarget(String ip) {
        this(ip, AgentConnection.DEFAULT_SOCKET_PORT);
    }

    @Override
    public void determineOs() {
        os = Environment.OS.LINUX;
        Connection c = ConnectionPool.get(ConnectionPool.CONNECTION_TYPES.SOCKET, this);

        if (c.isOpen()) {
            c.run("get-os");
        }
        // TODO: read response.. but for now only testing linux (/UNIX)
    }

    @Override
    public String getTargetName() {
        // TODO: check if IP is unique and enough to determine target, maybe we want to inject on a deeper level
        // and target some ID of a process (not in hostname?, change hostname usage to getTargetId)
        // or, see how we can handle multi-sut with one agent
        // Conclusion for now:
        return host + ":" + port;
    }

    @Override
    public String getHost() {
        return host;
    }


    @Override
    public Integer getPort() {
        return port;
    }


    @Override
    public boolean isLocal() {
        return false;
    }
}
