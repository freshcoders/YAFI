package nl.freshcoders.fit.connection.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class SocketIoImpl implements SocketIo, Runnable {
    Queue<String> messageQueue = new ConcurrentLinkedQueue<>();

    BufferedReader reader;
    BufferedWriter writer;

    Logger log = Logger.getLogger("Socket");

    public Long lastReceived = 0L;

    private boolean listening = true;

    public SocketIoImpl(BufferedWriter writer, BufferedReader reader) {
        this.reader = reader;
        this.writer = writer;
    }

    public boolean sendMessage(String message) {
        try {
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public void stopListening() {
        listening = false;
    }
    @Override
    public void startListening() {
        listening = true;
    }

    public String receiveMessage() {
        return messageQueue.poll();
    }

    public int getQueueSize() {
        return messageQueue.size();
    }

    @Override
    public void run() {
        while (listening) {
            try {
//                if (reader.ready() == false)
//                    continue;
                final String message = reader.readLine();
                lastReceived = System.currentTimeMillis();

                 if (message == null) {
                    continue;
                }

                messageQueue.add(message);
            } catch (IOException e) {
                // the socked was closed.. should have received exit message
//                throw new RuntimeException(e);
            }

        }
        // XXX: wait for some time for exit-ack?
    }
}
