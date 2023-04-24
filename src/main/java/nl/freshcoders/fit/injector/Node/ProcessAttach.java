package nl.freshcoders.fit.injector.Node;


import nl.freshcoders.fit.connection.LocalConnection;

/**
 *
 */
public class ProcessAttach {

    public ProcessInfo process;

    public ProcessAttach() {
        String partialProcessString = System.getProperty("process-string", ".jar");
        refreshFromJps(partialProcessString); // refresh the pidList once
    }

    // Method to refresh the pidList from jps command
    public void refreshFromJps(String mustContain) {
        String commandOutput = LocalConnection.executeCommand("ps x -o pid,command");
//        System.out.println("jps -l | grep " + mustContain + " result: " + commandOutput);
        if (commandOutput == null) {
            // log info no process
            return;
        }
        String uid = System.getProperty("UID", "");

        int pid = commandOutput.lines()
                .filter(line -> line.contains(mustContain))
                .filter(line -> line.contains(uid))
                // Use mapToInt to parse the PID as an integer
                .mapToInt(line -> Integer.parseInt(line.trim().split(" ")[0]))
                .findFirst()
                .orElse(0);
        process = new ProcessInfo(
                        pid, Ports.BYTEMAN
                );
    }

    public class ProcessInfo {
        public final Integer pid;
        public final Integer port;

        ProcessInfo(Integer pid, Integer port) {
            this.pid = pid;
            this.port = port;
        }

        @Override
        public String toString() {
            return "ProcessInfo{" +
                    "pid=" + pid +
                    ", port=" + port +
                    '}';
        }
    }

}

