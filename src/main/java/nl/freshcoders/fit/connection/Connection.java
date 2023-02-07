package nl.freshcoders.fit.connection;

public interface Connection {

    /**
     * Run a command on a connector.
     * @param command
     */
    void run(String command);

    boolean isOpen();

    void close();
}
