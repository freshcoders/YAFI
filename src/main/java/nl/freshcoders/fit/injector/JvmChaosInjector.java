package nl.freshcoders.fit.injector;


import nl.freshcoders.fit.connection.Connection;
import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.connection.LocalConnection;
import nl.freshcoders.fit.environment.Environment;
import nl.freshcoders.fit.helper.DependencyUtil;
import nl.freshcoders.fit.injector.Node.ProcessAttach;
import nl.freshcoders.fit.target.Target;

import java.util.logging.Logger;

public class JvmChaosInjector extends Injector {


    private boolean attached = false;

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

    public void installByteman() {
        ProcessAttach processAttacher = new ProcessAttach();

        ProcessAttach.ProcessInfo processInfo = processAttacher.process;
        Integer pid = processInfo.pid;
        Integer port = processInfo.port;
        System.out.println(processInfo);
        if (pid == 0) {
            Logger.getLogger("JvmChaos").warning("No application target found, verify string and execution order");
            return;
        }
        String i = LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bminstall.sh -p " + port + " -b -Dorg.jboss.byteman.transform.all " + pid);
//        String ii = LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -p " + port + " -b " + System.getProperty("datadir") + "/build/libs/fit-1.0-SNAPSHOT.jar");
        String iii = LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -p " + port + " -u");
        System.out.println(".."  + i);
//        System.out.println(ii);
        System.out.println(iii);
//            LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -p " + port + " -l " + System.getProperty("datadir") + "/rule_trace-all.btm2");

    }

    public String injectRule(Integer eventId) {
        if (Environment.OS.UNKNOWN == target.getOs())
            return null;

        if (!target.isLocal()) {
            Connection connection = getConnection();
//            System.out.println("sending submit message on: " + this.target.getTargetName());
            connection.run("submit:" + eventId);
        } else {
            System.out.println("checkVBM");
            // Check if we can inject, move this to the init
            if (!DependencyUtil.validBytemanInstall()) {
                throw new RuntimeException("BYTEMAN MISSING");
            }
            // Check if we are in a scenario that boots the SUT, or we attach?
            // Attach to the PID.
            // For now, we just find the pid by looking for the main class
            ProcessAttach processAttacher = new ProcessAttach();

            ProcessAttach.ProcessInfo processInfo = processAttacher.process;

            String btmFile = System.getProperty("datadir") + "/rule_" + eventId + ".btm";
            Integer pid = processInfo.pid;
            Integer port = processInfo.port;
            if (pid == 0) {
                return null;
            }

            String result = LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -p " + port + " -l " + btmFile);
            System.out.println("Injection: " + eventId + " completed with\n" + result);
            return result;
        }
        return null;
    }

    public String clearRule(Integer eventId) {
        Connection connection = getConnection();
        if (Environment.OS.UNKNOWN == target.getOs())
            return null;

        if (!target.isLocal()) {
            connection.run("clear:" + eventId);
        } else {
            if (!DependencyUtil.validBytemanInstall()) {
                throw new RuntimeException("BYTEMAN MISSING");
            }
            ProcessAttach processAttacher = new ProcessAttach();
            ProcessAttach.ProcessInfo processInfo = processAttacher.process;

            String btmFile = System.getProperty("datadir") + "/rule_" + eventId + ".btm";
            Integer pid = processInfo.pid;
            Integer port = processInfo.port;
            if (pid == 0) {
                return null;
            }

            LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -p " + port + " -u " + btmFile);
            return "true";
        }
        return null;
    }

    public void clear() {
        LocalConnection.executeCommand(BYTEMAN_HOME + "/bin/bmsubmit.sh -u ");
    }
}
