package nl.freshcoders.fit.injector;

import nl.freshcoders.fit.connection.AgentConnection;
import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.connection.LocalConnection;
import nl.freshcoders.fit.connection.socket.RemoteSocketIoImpl;
import nl.freshcoders.fit.connection.socket.SocketIo;
import nl.freshcoders.fit.connection.socket.SocketIoImpl;
import nl.freshcoders.fit.helper.DependencyUtil;
import nl.freshcoders.fit.injector.Node.Ports;
import nl.freshcoders.fit.target.RemoteTarget;
import nl.freshcoders.fit.target.Target;

import java.io.IOException;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import static nl.freshcoders.fit.helper.DependencyUtil.BYTEMAN_HOME;

public class InjectorEngine implements Runnable {

    Socket commandSocket;

    Target target;
    Timer timeoutClock = new Timer();

    RemoteSocketIoImpl remoteSocketIo;

    Long previousMessage = 0L;

//    {
//        try {
//            remoteSocketIo = new RemoteSocketIoImpl(1099);
//        } catch (RemoteException e) {
//            throw new RuntimeException(e);
//        }
//    }

    Target orchestrator = new RemoteTarget("orchestrator");

    final long TIMEOUT_DURATION = 30_000L;

    Queue<String> messageQueue = new LinkedList<>();

    public InjectorEngine(Socket commandSocket, Target target) {
        this.commandSocket = commandSocket;
        this.target = target;
    }

    private void initializeSocket() {
        AgentConnection orchestratorConnection = new AgentConnection(orchestrator);
        orchestratorConnection.updateFromSocket(commandSocket);
        ConnectionPool.putAgentConnection(orchestrator, orchestratorConnection);
    }

    public void registerRmiService() {
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("rmi://localhost:1099/socketIo", remoteSocketIo);
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    public void updateBmState(MessageHandler mh) {
        String bmSubmitResult = LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -p " + Ports.BYTEMAN);
        if (bmSubmitResult == null) {
            mh.bytemanConnected = false;
            return;
        }
        if (bmSubmitResult.startsWith("no rules installed")) {
            mh.bytemanConnected = true;
        } else {
            mh.bytemanConnected = false;
        }
    }

    @Override
    public void run() {
        System.out.println("run..");
        initializeSocket();
//        registerRmiService();
        refreshTimeout();
        SocketIo socketIo = ConnectionPool.getAgentConnection(orchestrator).socketIo;
        MessageHandler messageHandler = new MessageHandler(socketIo, target);

        System.out.println("started listening on thread for socket" + commandSocket.toString());
        Thread readerThread = new Thread(socketIo);
        readerThread.start();

        new Thread(messageHandler).start();
        while (!commandSocket.isClosed()) {
            updateBmState(messageHandler);{
                readerThread = new Thread(socketIo);
                readerThread.start();
            }
            if (!readerThread.isAlive())
            if (!((SocketIoImpl) socketIo).lastReceived.equals(previousMessage)) {
                previousMessage = ((SocketIoImpl) socketIo).lastReceived;
                refreshTimeout();
            }
            if (messageHandler.shouldExit) {
                socketIo.stopListening();
                close();
                return;
            }
        }
        System.out.println("socket broke");
    }

    private void refreshTimeout() {
        timeoutClock.cancel();
        timeoutClock = new Timer();
        timeoutClock.schedule(new TimerTask() {
            @Override
            public void run() {
                close();
            }
        }, TIMEOUT_DURATION);
    }

    public void close() {
        try {
            commandSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
