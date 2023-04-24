package nl.freshcoders.fit.connection.socket;

import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.target.RemoteTarget;
import nl.freshcoders.fit.target.Target;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

    public class RemoteSocketIoImpl extends UnicastRemoteObject implements RemoteSocketIo {
    Queue<String> messageQueue = new LinkedList<>();

    static AtomicInteger sequenceId = new AtomicInteger(0);

    public RemoteSocketIoImpl(int port) throws RemoteException {
        super(port);
    }


    public void sendMessage(String message) throws RemoteException {
        Target orchestrator = new RemoteTarget("orchestrator");
        Socket agentConnection = ConnectionPool.getAgentConnection(orchestrator).connection.orElse(null);
        if (agentConnection == null) {
            return;
        }
        try {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(agentConnection.getOutputStream()));
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessageBlocking(String message) throws RemoteException {
        Target orchestrator = new RemoteTarget("orchestrator");
        Socket agentConnection = ConnectionPool.getAgentConnection(orchestrator).connection.orElse(null);
        if (agentConnection == null) {
            return;
        }
        try {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(agentConnection.getOutputStream()));
            String idPrefix = sequenceId.addAndGet(1) + ":";
            writer.write(idPrefix + message + "\n");
            writer.flush();
            // now wait for the response with the same id
            // TODO: add some buffer that can be read on multiple threads
            // so it's thread safe and we dont "steal" other thread's message
            // maybe with a cursor for the buffer, or copies of the buffer.
            // The copy could work as:
            // 1. this method is called
            // 2. we register ourselves as a "reader thread"
            // 3. the agent reader thread reads a message and adds it to our queue
            // 4. we read our queue until message has the same prefix
            // 5. we de-register from the queue
            String msg = receiveMessage();

            while (!msg.startsWith(idPrefix)) {
                // XXX: could read slower.
                msg = receiveMessage();
            }
            String a = msg.split(idPrefix, 1)[1];
            // a might include a fault rule? install it?
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String receiveMessage() throws RemoteException {
        return messageQueue.poll();
    }

}
