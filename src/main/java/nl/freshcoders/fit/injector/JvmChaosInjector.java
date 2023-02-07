package nl.freshcoders.fit.injector;


import nl.freshcoders.fit.connection.Connection;
import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.connection.LocalConnection;
import nl.freshcoders.fit.environment.Environment;
import nl.freshcoders.fit.helper.DependencyUtil;
import nl.freshcoders.fit.injector.Node.ProcessStore;
import nl.freshcoders.fit.target.Target;

public class JvmChaosInjector extends Injector {


    public JvmChaosInjector(Target target) {
        super(target);
    }

    @Override
    public ConnectionPool.CONNECTION_TYPES connectionType() {
        return ConnectionPool.CONNECTION_TYPES.SOCKET;
    }

    @Override
    public SUPPORTED_INJECTORS type() {
        return SUPPORTED_INJECTORS.JVM;
    }

    public final String BYTEMAN_HOME = System.getenv("BYTEMAN_HOME");

    /**
     * See how we would want to throw an exception
     */
    public String injectException() {
        // TODO, move this into somewhere it runs once, since the environment won't be changed during run.
        // prepareInject();
        // injectRemote...();
        if (BYTEMAN_HOME == null) {
            System.out.println("Byteman home (BYTEMAN_HOME environment variable) is not set!");
        }
        // Do this in prepareInject (up to run)
        Connection connection = getConnection();
        if (Environment.OS.UNKNOWN == target.getOs())
            return null;
        if (!target.isLocal())
            connection.run("submit:run.btm");
        else {
            // Check if we can inject
            if (!DependencyUtil.validBytemanInstall()) {
                throw new RuntimeException("BYTEMAN MISSING");
            }
            // Check if we are in a scenario that boots the SUT, or we attach?
            // Attach to the PID.
            // For now, we just find the pid by looking for the main class
            String btmFile = "/home/vagrant/run.btm";

            LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -u " + btmFile);
            LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -l " + btmFile);

            System.out.println("locally executed!");
            return String.valueOf("true");
        }
        return null;
    }

    public void installByteman() {
        ProcessStore processStore = new ProcessStore();

        for (ProcessStore.ProcessInfo processInfo : processStore.getPidList()) {
            Integer pid = processInfo.pid;
            Integer port = processInfo.port;
            if (pid == 0) {
                continue;
            }
            System.out.println(pid);
            LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bminstall.sh -p " + port + " -b -Dorg.jboss.byteman.transform.all " + pid);
            LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -p " + port + " -b " + System.getProperty("datadir") + "/build/libs/fit-1.0-SNAPSHOT.jar");
            System.out.println("Clearing existing rules");
            LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -p " + port + " -u");
            System.out.println("installing all rule\n" + BYTEMAN_HOME + "/bin/bmsubmit.sh -p " + port + " -l " + System.getProperty("datadir") + "/rule_trace-all.btm");
//            LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -p " + port + " -l " + System.getProperty("datadir") + "/rule_trace-all.btm2");
        }
    }

    public String injectDelay(Integer eventId) {
        Connection connection = getConnection();
        if (Environment.OS.UNKNOWN == target.getOs())
            return null;

        if (!target.isLocal()) {
            System.out.println("[" + target.getTargetName() + "] as target, sending command!");

            connection.run("submit:" + eventId);
        } else {
            // Check if we can inject, move this to the init
            if (!DependencyUtil.validBytemanInstall()) {
                throw new RuntimeException("BYTEMAN MISSING");
            }
            // Check if we are in a scenario that boots the SUT, or we attach?
            // Attach to the PID.
            // For now, we just find the pid by looking for the main class
            ProcessStore processStore = new ProcessStore();

            String btmFile = System.getProperty("datadir") + "/rule_" + eventId + ".btm";
            for (ProcessStore.ProcessInfo processInfo : processStore.getPidList()) {
                Integer pid = processInfo.pid;
                Integer port = processInfo.port;
                if (pid == 0) {
                    continue;
                }

                LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -p " + port + " -u " + btmFile);
                LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -p " + port + " -l " + btmFile);
            }
            System.out.println("locally executed!");
            return String.valueOf("true");
        }
        return null;
    }

    public void clear() {
        LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -u ");
    }
}
