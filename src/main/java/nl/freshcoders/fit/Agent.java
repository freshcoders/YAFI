package nl.freshcoders.fit;

import nl.freshcoders.fit.connection.AgentConnection;
import nl.freshcoders.fit.helper.DependencyUtil;
import nl.freshcoders.fit.injector.InjectorEngine;
import nl.freshcoders.fit.target.LocalTarget;
import nl.freshcoders.fit.target.Target;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

public class Agent {

    private static Agent instance;

    Target thisNode;

    ServerSocket socketConnection;

    private Agent() {
        try {
            Integer socketPort = Integer.valueOf(System.getProperty("SOCKET_PORT", AgentConnection.DEFAULT_SOCKET_PORT.toString()));
            String uid = System.getProperty("UID");

            thisNode = new LocalTarget(0, uid);
            socketConnection = new ServerSocket(socketPort, 1);

        } catch (NumberFormatException numberFormatException) {
            Logger.getLogger("AGENT").severe("Invalid SOCKET_PORT property provided, must be numerical");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void run() {
        final int poolSize = 1;
        Executor executor = Executors.newFixedThreadPool(poolSize);
        Semaphore semaphore = new Semaphore(poolSize);
        // for now, we check Byteman states in the main loop and thus it should be installed
        boolean runnable = DependencyUtil.validBytemanInstall();
        if (!runnable) {
            System.exit(1);
        }

        while (true) {
            try {
                semaphore.acquire(); // Acquire a permit, or wait until one is available
                Socket socket = socketConnection.accept();
                executor.execute(() -> {
                    try {
                        new InjectorEngine(socket, thisNode).run();
                    } finally {
                        semaphore.release(); // Release the permit when the task is done
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
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
