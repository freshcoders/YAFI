package nl.freshcoders.fit.connection.socket;

public interface SocketIo extends Runnable {

    String receiveMessage();

    boolean sendMessage(String msg);

    void stopListening();
    void startListening();
}
