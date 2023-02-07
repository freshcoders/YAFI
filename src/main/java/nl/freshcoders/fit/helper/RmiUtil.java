package nl.freshcoders.fit.helper;

import nl.freshcoders.fit.connection.socket.RemoteSocketIo;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiUtil  {

    private RmiUtil() {
        //p
    }
    public static void sendTraceEvent(Object clazz, String location) {
        try{
            Registry registry = LocateRegistry.getRegistry();
            RemoteSocketIo stub= (RemoteSocketIo) registry.lookup("rmi://localhost:1099/socketIo");
            stub.sendMessage(clazz.getClass() + location);
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }


    public static void sendTraceEventBlocking(Object clazz, String location) {
        try{
            Registry registry = LocateRegistry.getRegistry();
            RemoteSocketIo stub= (RemoteSocketIo) registry.lookup("rmi://localhost:1099/socketIo");
            stub.sendMessage(clazz.getClass() + location);
            stub.receiveMessage();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    public static void sendTraceEvent(String location) {
        try {
            Registry registry = LocateRegistry.getRegistry();

            RemoteSocketIo stub= (RemoteSocketIo) registry.lookup("rmi://localhost:1099/socketIo");
            stub.sendMessage( location);
        } catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

}
