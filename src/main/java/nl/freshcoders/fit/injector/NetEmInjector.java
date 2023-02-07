package nl.freshcoders.fit.injector;


import nl.freshcoders.fit.connection.Connection;
import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.connection.LocalConnection;
import nl.freshcoders.fit.environment.Environment;
import nl.freshcoders.fit.target.Target;

public class NetEmInjector extends Injector {

    public NetEmInjector(Target target) {
        super(target);
    }

    @Override
    public ConnectionPool.CONNECTION_TYPES connectionType() {
        return ConnectionPool.CONNECTION_TYPES.SOCKET;
    }

    @Override
    public SUPPORTED_INJECTORS type() {
        return SUPPORTED_INJECTORS.NETEM;
    }

    public void injectLatency(String... args) {
        Connection connection = getConnection();

        if (Environment.OS.UNKNOWN == target.getOs())
            return;

        // currently only support Linux / tc (ssh)
        // todo: fix and sanitize?
        if (!target.isLocal()) {
            connection.run("submit");
        }
        else {
            System.out.println("we are local");
            String x = LocalConnection.executeCommand("sudo tc qdisc add dev eth0 root netem delay 1000ms");
            System.out.println(x);
        }
    }

    public void clear() {
        String x = LocalConnection.executeCommand("sudo tc qdisc del dev eth0 root");
    }


}
