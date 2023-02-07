package nl.freshcoders.fit.injector;

import jdk.jshell.spi.ExecutionControl;
import nl.freshcoders.fit.connection.Connection;
import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.target.Target;

/**
 * Base class for any custom injector. Follow the documentations to comply with the expected format and
 */
public abstract class Injector {
    public Target target;

    public Injector(Target target) {
        this.target = target;
    }

    public ConnectionPool.CONNECTION_TYPES connectionType() throws ExecutionControl.NotImplementedException {
        throw new ExecutionControl.NotImplementedException("Implement the injection interface in the injector: " +  this.getClass().getName());
    }

    public SUPPORTED_INJECTORS type() throws ExecutionControl.NotImplementedException {
        throw new ExecutionControl.NotImplementedException("Implement the injector type in the injector: " +  this.getClass().getName());
    }

    /**
     * Get a connection to the target from the pool
     * TODO: Replace with actual connection getter?
     * @return
     */
    public Connection getConnection() {
        Connection connection = null;
        try {
            connection = ConnectionPool.get(this.connectionType(), this.target);
        } catch (ExecutionControl.NotImplementedException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public enum SUPPORTED_INJECTORS {
        JVM,
        NETEM
    }
}
