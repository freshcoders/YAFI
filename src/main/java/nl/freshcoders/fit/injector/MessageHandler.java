package nl.freshcoders.fit.injector;

import nl.freshcoders.fit.connection.socket.SocketIo;
import nl.freshcoders.fit.environment.EnvironmentDetector;
import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.parser.PlanParser;
import nl.freshcoders.fit.target.Target;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.Map;

public class MessageHandler implements Runnable {
    String buffer;

    SocketIo socketIo = null;

    public boolean receivingFile = false;

    FileWriter fw = null;

    Target target;

    public boolean shouldExit = false;
    public long lastClockSent = 0;

    public boolean bytemanConnected = false;

    public MessageHandler(SocketIo socketIo, Target target) {
        this.buffer = "";
        this.socketIo = socketIo;
        this.target = target;
    }

    @Override
    public void run() {
        try {
            while (!shouldExit) {
                final String message = socketIo.receiveMessage();
                if (message == null) {
                    // send clock?
                    RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
                    // Get start time, might update whenever byteman reconnects
                    long startTime = bean.getStartTime();
                    long l = Instant.now().toEpochMilli() - startTime;
                    if (l - (l % 10) != lastClockSent)
                        socketIo.sendMessage("event:clock:" + (l - (l % 10)));
                    lastClockSent = l - (l % 10);
                    continue;
                }

                if (message.equals("ping")) {
                    // ignore for now?
                    continue;
                }
                if (!receivingFile)
                    System.out.println("message=" + message);

                if (message.equals("exit")) {
//                    socketIo.sendMessage("exit_ack");
                    socketIo.stopListening();
                    shouldExit = true;
                    break;
                }
                if (receivingFile) {
                    if (!message.contains("file:transfer:end")) {
                        if (message != null)
                            buffer += message + "\n";
                    } else {
                        System.out.println("fully received file");
                        DumperOptions options = new DumperOptions();
                        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
                        Yaml yaml = new Yaml(options);
                        try {
                            Map<String, Object> data = yaml.load(buffer);
                            yaml.dump(data, fw);
                        } catch (ParserException exception) {
                            // something went wrong with parsing the received file
                            buffer = "";
                            socketIo.sendMessage("error:file:transfer");
                            continue;
                        }
                        socketIo.sendMessage("success:file:transfer");
                        fw.close();
                        receivingFile = false;
                        if (message.endsWith(":plan.yml")) {
                            // failure plan received, we need to parse and install
                            // rules, so we can inject faults
                            String file = message.split("file:transfer:end:")[1];
                            FileInputStream reader = new FileInputStream(System.getProperty("datadir") + "/" + file);
                            FailurePlan failurePlan = PlanParser.fromInputStream(reader);
                            failurePlan.install();
                            continue;
                        }
                    }
                    continue;
                }
                if (message.equals("get-os")) {
                    System.out.println("getting os");
                    String response = EnvironmentDetector.getOperatingSystem().toString();
                    socketIo.sendMessage(response);
                } else if (message.contains("byteman:attach")) {
//                    new Thread(() -> {
                        JvmChaosInjector injector = new JvmChaosInjector(target);
                        injector.installByteman();
//                    }).start();
                } else if (message.contains("byteman:clear")) {
                    new Thread(() -> {
                        JvmChaosInjector injector = new JvmChaosInjector(target);
                        injector.clear();
                        socketIo.sendMessage("log:"+System.currentTimeMillis()+":clearing:ALL");
                    }).start();
                    new Thread(() -> {
                        NetEmInjector injector = new NetEmInjector(target);
                        injector.clear();
                    }).start();
                } else if (message.contains("submit")) {
                    new Thread(() -> {
                        String eventId = message.split("submit:")[1];
                        socketIo.sendMessage("log:"+System.currentTimeMillis()+":installing: " + eventId);
                        System.out.println("submitting rule into java program");
//                        NetEmInjector injector = new NetEmInjector(target);
//                        injector.injectLatency();
                        JvmChaosInjector injector = new JvmChaosInjector(target);
                        String response = injector.injectRule(Integer.valueOf(eventId));
//                        socketIo.sendMessage(response);
                        socketIo.sendMessage("log:"+System.currentTimeMillis()+":installed: " + eventId + "|"+response);
                    }).start();
                } else if (message.contains("file:transfer:start")) {
                    String file = message.split("file:transfer:start:")[1];
                    if (file.equals("plan.yml")) {
                        receivingFile = true;
                        System.out.println("NOW in receiving file mode");
                        fw = new FileWriter(System.getProperty("datadir") + "/" + file);
                        socketIo.sendMessage(file);
                    }
                } else {
                    socketIo.sendMessage("unrecognised command: " + message);
                }
            }
        } catch (IOException se) {

        }
    }
}
