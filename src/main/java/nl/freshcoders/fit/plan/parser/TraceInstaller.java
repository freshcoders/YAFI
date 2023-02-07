package nl.freshcoders.fit.plan.parser;

import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.event.fault.Occurrence;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class TraceInstaller {

        public static void parseBytemanTrigger(ConditionalEvent event) {
            Occurrence.Location location = event.getFault().getOccurrence().getLocation();
            System.out.println(event);
            // Generate the Byteman rule file.
            System.out.println("installing bm : " + System.getProperty("datadir") + "/rule_" + event.toHash() + ".btm");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(System.getProperty("datadir") + "/rule_" + event.toHash() + ".btm"))) {
                // Trace the execution of the specified method.
                writer.write(String.format("RULE eventId %d\n", event.toHash()));
                String classFQDN =  location.getClassName();
                if (location.getClassName().startsWith("interface ")) {
                    classFQDN = "INTERFACE " + location.getClassName().substring(10);
                }
                if (location.getClassName().startsWith("class ")) {
                    classFQDN = "CLASS " + location.getClassName().substring(6);
                }

                writer.write(String.format("%s\n", classFQDN));
                if (location.getMethod() != null)
                    writer.write(String.format("METHOD %s\n", location.getMethod()));
                else
                    writer.write("METHOD <init>");
                writer.write("AT ENTRY\n");
                writer.write("IF TRUE\n");
                writer.write("DO traceln(\"---\");\n");
                if (event.getFault().getType().equals("delay"))
                    writer.write("  Thread.sleep(" + event.getFault().getConfig() + ");\n");

                writer.write("ENDRULE\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
}