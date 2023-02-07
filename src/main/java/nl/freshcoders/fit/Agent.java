package nl.freshcoders.fit;

import nl.freshcoders.fit.connection.AgentConnection;
import nl.freshcoders.fit.injector.InjectorEngine;
import nl.freshcoders.fit.target.LocalTarget;
import nl.freshcoders.fit.target.Target;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

public class Agent {

    private static Agent instance;

    Target thisNode = new LocalTarget();

    ServerSocket socketConnection;

    private Agent() {
        try {
            Integer socketPort = Integer.valueOf(System.getProperty("SOCKET_PORT", AgentConnection.DEFAULT_SOCKET_PORT.toString()));
            socketConnection = new ServerSocket(socketPort, 1);

        } catch (NumberFormatException numberFormatException) {
            Logger.getLogger("AGENT").severe("Invalid SOCKET_PORT property provided, must be numerical");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void run() {
        final int poolSize = 2;
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);
        while (true) {
            if (executor.getActiveCount() >= poolSize) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            try {
                Socket socket = socketConnection.accept();
                executor.execute(new InjectorEngine(socket, thisNode));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Agent getInstance() {
        if (instance == null) {
            instance = new Agent();
        }
        return instance;
    }
}
