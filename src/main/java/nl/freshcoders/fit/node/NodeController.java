package nl.freshcoders.fit.node;

import jdk.jshell.spi.ExecutionControl;
import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.injector.Injector;
import nl.freshcoders.fit.injector.JvmChaosInjector;
import nl.freshcoders.fit.injector.NetEmInjector;
import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.target.Target;

import java.util.HashMap;

public class NodeController {
    public Target target;

    private boolean sutAttached = false;
    public HashMap<Injector.SUPPORTED_INJECTORS, Injector> injectors = new HashMap<>();

    public NodeController(Target target) {
        this.target = target;
        this.target.determineOs();

    }

    public void tick() {

    }


    /**
     * Set up the injectors needed for injection.
     */
    public void initialize() {
        JvmChaosInjector injector = new JvmChaosInjector(target);
//        registerInjector(new NetEmInjector(target));
        registerInjector(injector);
    }

    public void registerInjector(Injector i) {
        // Any registered injectors have to be checked to see if they implement their type and injection interface.
        try {
            String connectionType = String.valueOf(i.connectionType());
            String injectionType = String.valueOf(i.type());
            this.injectors.put(i.type(), i);
            // Establish connection on the required channel for future injections.
            ConnectionPool.get(i.connectionType(), target);
        } catch (ExecutionControl.NotImplementedException e) {
            throw new RuntimeException("Set up the connection type and injection type for all injectors! " + e.getMessage());
        }
    }

    public void inject(Integer eventId, String type, String... args) {
        Injector injector;
        // XXX: replace with different construct, if possible (use enums?)
        switch (type) {
            case "latency":
                injector = injectors.get(Injector.SUPPORTED_INJECTORS.NETEM);
                ((NetEmInjector) injector).injectLatency(args);
                break;
            case "delay":
            case "enum-switch":
            case "exception":
            case "fixed-clock":
            default:
                injector = injectors.get(Injector.SUPPORTED_INJECTORS.JVM);
//                    ((JvmChaosInjector) injector).getConnection().run("byteman:attach");
                ((JvmChaosInjector) injector).injectRule(eventId);
                break;
        }
    }

    public void clearInjection(Integer eventId) {
        Injector injector;
        injector = injectors.get(Injector.SUPPORTED_INJECTORS.JVM);
        ((JvmChaosInjector) injector).clearRule(eventId);
    }

    public boolean validateConnectionsForPlan(FailurePlan plan) {
        if (this.target.isLocal())
            return true;
        // todo: parse some plan actions that require ssh/socket
        return ConnectionPool.get(ConnectionPool.CONNECTION_TYPES.SOCKET, this.target).isOpen();
    }
}
