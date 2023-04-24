package nl.freshcoders.fit.injector.Node;

public class Ports {
    public static final Integer BYTEMAN =
            Integer.valueOf(System.getProperty("bm-port-offset", "100"))
                    + Integer.valueOf(System.getProperty("SOCKET_PORT", "13013"));


}
