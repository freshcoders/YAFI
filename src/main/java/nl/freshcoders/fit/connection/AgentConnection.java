package nl.freshcoders.fit.connection;

import nl.freshcoders.fit.connection.socket.SocketIo;
import nl.freshcoders.fit.connection.socket.SocketIoImpl;
import nl.freshcoders.fit.target.Target;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Optional;

/**
 * Remotely execute commands through a Socket?.
 */
public class AgentConnection extends RemoteConnection<Socket> {

    public static final Integer DEFAULT_SOCKET_PORT = 13013;
    public BufferedWriter writer;
    public BufferedReader reader;
    public Logger logger;

    public SocketIo socketIo;

    private Thread messageReader;

    public AgentConnection(Target target) {
        this(target.getHost(), target.getPort());
    }

    public AgentConnection(String ip, Integer port) {
        super(ip, port);
        logger = Logger.getLogger("AgentConnection");
    }

    public void inject(String command) {

    }

    public String receive() {
        return socketIo.receiveMessage();
    }

    public void listen() {
        messageReader = new Thread(socketIo);
        messageReader.start();
    }

    public void run(String command) {
        if (!isOpen()) {
            System.out.println("no socket available");
            return;
        }
        socketIo.sendMessage(command);
    }

    @Override
    public boolean isOpen() {
        boolean open = connection.map(Socket::isConnected).orElse(false);

        return open;
    }

    @Override
    public void close() {
        if (!isOpen())
            return;
        // TODO: add check for active connection?
        try {
            socketIo.stopListening();
            socketIo.sendMessage("exit");
            messageReader.join();
            connection.orElseThrow().close();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateFromSocket(Socket socket) {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            connection = Optional.of(socket);
            socketIo = new SocketIoImpl(writer, reader);
            listen();
        } catch (IOException e) {
            throw new RuntimeException("Could not update from socket", e);
        }
    }

    @Override
    public Optional<Socket> setupConnection() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 1000);
            if (!socket.isConnected() || socket.isClosed()) {
                logger.warn("no socket opened when trying to connect to " + ip + ":" + port.toString());
                return Optional.empty();
            }
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            socketIo = new SocketIoImpl(writer, reader);
            listen();
            return Optional.of(socket);
        } catch (SocketTimeoutException e) {
            logger.error("Socket timed out for " + ip + ":" + port.toString());
        } catch (ConnectException e) {
            logger.error("Socket couldn't make connection to " + ip + ":" + port.toString());
//            throw new RuntimeException("Socket not listening! " + e.getMessage());
        } catch (IOException e) {
            // retry?
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

}
