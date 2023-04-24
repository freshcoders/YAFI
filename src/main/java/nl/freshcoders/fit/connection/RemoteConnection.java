package nl.freshcoders.fit.connection;

import java.util.Optional;

public abstract class RemoteConnection<T> implements Connection {
    protected String ip;
    protected Integer port;

    public Optional<T> connection = Optional.empty();

    public RemoteConnection(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    protected final Optional<T> getConnection() {
        // TODO: verify if the connection is still live
        if (!isOpen()) {
            while (connection.isEmpty()) {
                connection = setupConnection();
            }
        }
        return connection;
    }

    abstract Optional<T> setupConnection();

    public boolean run(String s) {
        // NOOP
        return true;
    }
}
