package nl.freshcoders.fit.connection;

import nl.freshcoders.fit.target.Target;

import java.util.HashMap;
import java.util.Map;

public class ConnectionPool {
    public static void closeAll() {
        for (SshConnection conn : sshConnections.values()) {
            conn.close();
        }
        for (AgentConnection conn : agentConnections.values()) {
            conn.close();
        }
        sshConnections.clear();
        agentConnections.clear();
    }

    public enum CONNECTION_TYPES {
        SSH,
        SOCKET
    }

    private static Map<String, SshConnection> sshConnections = new HashMap<>();

    /**
     * List of active connections to agents. Underlying implementation can change.
     */
    private static Map<String, AgentConnection> agentConnections = new HashMap<>();

    public static RemoteConnection get(CONNECTION_TYPES type, Target target) {
        if (target.isLocal()) {
            return null;
        }
        if (target.getTargetName() == null) {
            throw new RuntimeException("no hostname on target");
        }
        RemoteConnection connection = null;
        switch (type) {
            case SSH:
                connection = getSshConnection(target);
                break;
            case SOCKET:
                connection = getAgentConnection(target);
                break;
            default:
                break;
        }
        return connection;
    }

    public static SshConnection getSshConnection(Target target) {
        SshConnection conn = sshConnections.get(target.getTargetName());
        if (conn == null) {
            conn = new SshConnection(target);
            sshConnections.put(target.getTargetName(), conn);
        }
        if (!target.getTargetName().startsWith("orchestrator"))
            conn.getConnection();
        return conn;
    }

    public static AgentConnection getAgentConnection(Target target) {
        AgentConnection conn = agentConnections.get(target.getTargetName());
        if (conn == null) {
            conn = new AgentConnection(target);
            agentConnections.put(target.getTargetName(), conn);
        }
        if (!target.getTargetName().startsWith("orchestrator"))
            conn.getConnection();
        return conn;
    }

    public static void putAgentConnection(Target target, AgentConnection conn) {
        agentConnections.put(target.getTargetName(), conn);
    }
}
