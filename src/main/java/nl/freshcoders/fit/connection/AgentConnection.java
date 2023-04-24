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
    public boolean closing = false;

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

    public boolean run(String command) {
        if (!isOpen()) {
            return false;
        }
        return socketIo.sendMessage(command);
    }

    @Override
    public boolean isOpen() {
        boolean open = connection.map(Socket::isConnected).orElse(false);

        return open || closing;
    }

    @Override
    public void close() {
        if (!isOpen()) {
            socketIo.stopListening();
            return;
        }
        closing = true;
        try {
            socketIo.stopListening();

            socketIo.sendMessage("exit");
//            // Send "exit" message until the socket closes
//            while(socketIo.sendMessage("exit")) {
//                Thread.sleep(100);
//            }

            messageReader.join();
            connection.orElseThrow().close();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            closing = false;
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
            socket.connect(new InetSocketAddress(ip, port), 2000);
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
            // We wait for a few seconds before retrying
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        } catch (ConnectException e) {
            logger.error("Socket couldn't make connection to " + ip + ":" + port.toString());
            // We wait for a few seconds before retrying
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
//            throw new RuntimeException("Socket not listening! " + e.getMessage());
        } catch (IOException e) {
            // retry?
            logger.error("[IO] " + ip + ":" + port.toString());
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

}
