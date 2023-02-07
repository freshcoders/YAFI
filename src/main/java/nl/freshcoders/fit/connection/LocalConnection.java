package nl.freshcoders.fit.connection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Should this be deleted? Local "connection" can be replaced by direct execution.
 */
public class LocalConnection {

    /**
     * Remove from Connector interface? or add local init stuff, like creating a process?
     * @param host
     * @return
     */
    public Optional<LocalConnection> setupConnection(String host) {
        return Optional.of(this);
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


    public void run(String command) {
        executeCommand(command);
    }
}
