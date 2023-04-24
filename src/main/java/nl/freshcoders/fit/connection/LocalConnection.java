package nl.freshcoders.fit.connection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Should this be deleted? Local "connection" can be replaced by direct execution.
 * All other (even local) connections can be seen as remote connections.
 */
public class LocalConnection implements Connection {

    /**
     * Remove from Connector interface? or add local init stuff, like creating a process?
     * @param host
     * @return
     */
    public Optional<LocalConnection> setupConnection(String host) {
        return Optional.of(this);
    }

    public static void executeCommandSilent(String cmd) {
        String result = null;
        Process exec = null;
        try {
            System.out.println(cmd);
            exec = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        while (exec.isAlive()) {
//            // waiting
//        }
//        Logger.getLogger("LocalConnection").info("Command executions: `" + cmd + "` completed");
    }

    public static String executeCommand(String cmd) {
        String result = null;
        try (InputStream inputStream = Runtime.getRuntime().exec(cmd).getInputStream();
             Scanner s = new Scanner(inputStream).useDelimiter("\\A")) {
            result = s.hasNext() ? s.next() : null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        Logger.getLogger("LocalConnection").info("Command executions: `" + cmd + "` completed with response: " + result);
        return result;
    }


    /**
     * Running a command on a localhost still is sent to the agent. This is to more
     * easily manage threads and application linking.
     *
     * @param command The command to send to the agent.
     * @return
     */
    public boolean run(String command) {
        executeCommand(command);
        return true;
    }

    /**
     * Always open
     * @return true
     */
    @Override
    public boolean isOpen() {
        return true;
    }

    /**
     * Nothing to close.
     */
    @Override
    public void close() {
        // noop
    }
}
