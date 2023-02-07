package nl.freshcoders.fit.connection.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketIoImpl implements SocketIo, Runnable {
    Queue<String> messageQueue = new ConcurrentLinkedQueue<>();

    BufferedReader reader;
    BufferedWriter writer;

    public Long lastReceived = 0L;

    private boolean listening = true;

    public SocketIoImpl(BufferedWriter writer, BufferedReader reader) {
        this.reader = reader;
        this.writer = writer;
    }

    public void sendMessage(String message) {
        try {
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                if (reader.ready() == false)
                    continue;
                lastReceived = System.currentTimeMillis();
                final String message = reader.readLine();

                if (message == null) {
                    continue;
                }
                messageQueue.add(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
