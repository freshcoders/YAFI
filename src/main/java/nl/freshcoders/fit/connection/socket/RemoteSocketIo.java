package nl.freshcoders.fit.connection.socket;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteSocketIo extends Remote {
    String receiveMessage() throws RemoteException;

    void sendMessage(String msg) throws RemoteException;
}
