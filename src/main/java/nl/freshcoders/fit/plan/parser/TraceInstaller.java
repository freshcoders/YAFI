package nl.freshcoders.fit.plan.parser;

import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.event.fault.Occurrence;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class TraceInstaller {

        public static void parseBytemanTrigger(ConditionalEvent event) {
            // Generate the Byteman rule file.
            String rule = makeRule(event);

            System.out.println("installing bm : " + System.getProperty("datadir") + "/rule_" + event.toHash() + ".btm");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(System.getProperty("datadir") + "/rule_" + event.toHash() + ".btm"))) {
                writer.write(rule);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private static String makeRule(ConditionalEvent event) {
            boolean singleExecution = false;
            String faultType = event.getFault().getType();
            String faultConfig = event.getFault().getConfig();

            Occurrence.Location location = event.getFault().getOccurrence().getLocation();

            StringBuilder builder = new StringBuilder();
            Integer eventId = event.toHash();
            builder.append(String.format("RULE eventId %d\n", eventId));
            String classFQDN = "CLASS " + location.getClassName();
            if (location.getClassName().startsWith("interface ")) {
                classFQDN = "INTERFACE " + location.getClassName().substring(10);
            }
            if (location.getClassName().startsWith("class ")) {
                classFQDN = "CLASS " + location.getClassName().substring(6);
            }
            if (location.getClassName().startsWith("abstract ")) {
                classFQDN = "CLASS ^" + location.getClassName().substring(9);
            }

            builder.append(String.format("%s\n", classFQDN));
            if (location.getMethod() != null)
                builder.append(String.format("METHOD %s\n", location.getMethod()));
            else
                builder.append("METHOD <init>");

            if (!faultType.contains("clock"))
                builder.append("AT ENTRY\n");
            else
                builder.append("AT RETURN\n");


            if (!singleExecution) {
                builder.append("IF TRUE\n");
            } else {
                builder.append("IF NOT flagged($this)\n");
            }
            builder.append("DO traceln(\"injection-event:" + eventId + "\");\n");

            if (singleExecution) {
                builder.append("  flag($this)\n");
            }
            switch (faultType) {
                case "exception":
                    builder.append("  throw new " + faultConfig + "(\"INJECTED EXCEPTION! from: (" + eventId + ")\");\n");
                    break;
                case "delay":
                    builder.append("  Thread.sleep(" + faultConfig + ");\n");
                    break;
                case "clock-skew":
                    builder.append("  return $! + " + faultConfig + ";\n");
                    break;
                case "fixed-clock":
                    builder.append("  return " + faultConfig + ";\n");
                    break;
                default:
                    break;
            }

            builder.append("ENDRULE\n");

            String result = builder.toString();
            return result;
        }
}