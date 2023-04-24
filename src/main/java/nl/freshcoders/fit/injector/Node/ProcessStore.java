package nl.freshcoders.fit.injector.Node;


import nl.freshcoders.fit.connection.LocalConnection;

import java.util.ArrayList;

/**
 * Will be phased out in favour of linking through UID.
 */
public class ProcessStore {

    ArrayList<ProcessInfo> pidList;

    final Integer BM_PORT_OFFSET;

    public ProcessStore() {
        BM_PORT_OFFSET = Integer.valueOf(System.getProperty("bm-port-offset", "9800"));
        String partialProcessString = System.getProperty("process-string", "java");
        pidList = new ArrayList<>();
        refreshFromJps(partialProcessString); // refresh the pidList once
    }

    // Method to refresh the pidList from jps command
    public void refreshFromJps(String mustContain) {
        pidList.clear();
        String commandOutput = LocalConnection.executeCommand( "jps -l");
        System.out.println("jps -l | grep " + mustContain + " result: " + commandOutput);
        if (commandOutput == null) {
            // log info no process
            return;
        }
        commandOutput.lines()
                .filter(line -> line.contains(mustContain))
                // Use mapToInt to parse the PID as an integer
                .mapToInt(line -> Integer.parseInt(line.split(" ")[0]))
                .forEach(pid -> pidList.add(
                        new ProcessInfo(
                                pid, freePort(BM_PORT_OFFSET)
                        )
                ));
    }

    public Integer freePort(Integer freePortOffset) {
        return getPidList().stream().anyMatch(o -> o.port.equals(freePortOffset)) ?
        freePort(freePortOffset+1) : freePortOffset;
    }

    // Getter for all pid values
    public ArrayList<ProcessInfo> getPidList() {
        return pidList;
    }

    public class ProcessInfo {
        public final Integer pid;
        public final Integer port;

        ProcessInfo(Integer pid, Integer port) {
            this.pid = pid;
            this.port = port;
        }
    }

}

