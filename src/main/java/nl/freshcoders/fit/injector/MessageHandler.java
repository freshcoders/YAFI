package nl.freshcoders.fit.injector;

import nl.freshcoders.fit.connection.socket.SocketIo;
import nl.freshcoders.fit.environment.EnvironmentDetector;
import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.parser.PlanParser;
import nl.freshcoders.fit.target.Target;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

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

    public MessageHandler(SocketIo socketIo, Target target) {
        this.buffer = "";
        this.socketIo = socketIo;
        this.target = target;
    }

    @Override
    public void run() {
        try {
            while (true) {
                final String message = socketIo.receiveMessage();
                if (message == null) {
                    // send clock?
                    continue;
                }
                System.out.println("message=" + message);

                if (message.equals("exit")) {
                    shouldExit = true;
                    socketIo.sendMessage("exit_ack");
                    continue;
                }
                if (receivingFile) {
                    if (!message.contains("file:transfer:end")) {
                        System.out.println("recv");
                        if (message != null)
                            buffer += message + "\n";
//                    socketIo.sendMessage(".");
                    } else {
                        System.out.println("fully received file");
                        DumperOptions options = new DumperOptions();
                        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
                        Yaml yaml = new Yaml(options);
                        Map<String, Object> data = yaml.load(buffer);
                        yaml.dump(data, fw);
                        socketIo.sendMessage("file written");
                        fw.close();
                        receivingFile = false;
                        if (message.endsWith(":plan.yml")) {
                            // failure plan received, we need to parse and install
                            // rules, so we can inject faults
                            String file = message.split("file:transfer:end:")[1];
                            FileInputStream reader = new FileInputStream(file);
                            FailurePlan failurePlan = PlanParser.fromInputStream(reader);
                            failurePlan.install();
                            continue;
                        }
                    }
                    continue;
                }
                if (message.equals("ping")) {
                    RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
                    // Get start time
                    long startTime = bean.getStartTime();
                    socketIo.sendMessage("event:clock:" + (Instant.now().toEpochMilli() - startTime));
                } else if (message.equals("get-os")) {
                    System.out.println("getting os");
                    String response = EnvironmentDetector.getOperatingSystem().toString();
                    socketIo.sendMessage(response);
                } else if (message.contains("byteman:attach")) {
                    new Thread(() -> {
                        System.out.println("installing byteman");
                        JvmChaosInjector injector = new JvmChaosInjector(target);
                        injector.installByteman();
                    }).start();
                } else if (message.contains("byteman:clear")) {
                    new Thread(() -> {
                        JvmChaosInjector injector = new JvmChaosInjector(target);
                        injector.clear();
                    }).start();
                    new Thread(() -> {
                        NetEmInjector injector = new NetEmInjector(target);
                        injector.clear();
                    }).start();
                } else if (message.contains("submit")) {
                    new Thread(() -> {
                        String eventId = message.split("submit:")[1];
                        System.out.println("submitting rule into java program");
//                        NetEmInjector injector = new NetEmInjector(target);
//                        injector.injectLatency();
                        JvmChaosInjector injector = new JvmChaosInjector(target);
                        String response = injector.injectDelay(Integer.valueOf(eventId));
//                        socketIo.sendMessage(response);
                    }).start();
                } else if (message.contains("file:transfer:start")) {
                    String file = message.split("file:transfer:start:")[1];
                    if (file.equals("plan.yml")) {
                        receivingFile = true;
                        System.out.println("NOW in receiving file mode");
                        fw = new FileWriter("./" + file);
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
