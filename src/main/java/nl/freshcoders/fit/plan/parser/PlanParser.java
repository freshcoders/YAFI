package nl.freshcoders.fit.plan.parser;

import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.event.ConditionalEvent;
import nl.freshcoders.fit.plan.event.fault.Fault;
import nl.freshcoders.fit.plan.event.fault.Occurrence;
import nl.freshcoders.fit.plan.event.trigger.Trigger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Parses valid data into a failure plan. Currently, only YAML is supported.
 * @see nl.freshcoders.fit.plan.FailurePlan
 */
public class PlanParser {

    public static FailurePlan fromInputStream(InputStream yaml) {
        String raw = new BufferedReader(
                new InputStreamReader(yaml, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        if (raw.isEmpty()) {
            Logger.getLogger("Parser").warning("file read as empty");
        }
        // Use the SnakeYAML library to parse the YAML string
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setPrettyFlow(true);
        Yaml yamlParser = new Yaml(options);
        Map<String, Object> data = yamlParser.load(raw);
        FailurePlan fp = new FailurePlan();
        fp.setRawPlan(raw);
        // Get the list of hosts
        List<Map<String, Object>> hosts = (List<Map<String, Object>>) data.get("hosts");

        for (Map<String, Object> host : hosts) {
            if (host.get("ip") != null && !host.get("ip").equals("127.0.0.1")) {
                fp.addHost((String) host.get("ip"), (Integer) host.get("port"));
            } else {
                fp.addLocalHost((Integer) host.get("port"), (String) host.get("uid"));
            }
        }


        // Get the list of conditional events from the parsed data
        List<Map<String, Object>> events = (List<Map<String, Object>>) data.get("events");

        // Iterate over the events and add them to the failure plan
        for (Map<String, Object> event : events) {
            // Create a new ConditionalEvent object
            ConditionalEvent ce = new ConditionalEvent();

            // Get the fault and trigger data from the parsed event
            Map<String, Object> faultData = (Map<String, Object>) event.get("fault");
            Map<String, Object> triggerData = (Map<String, Object>) event.get("trigger");

            if (faultData != null) {
                Map<String, Object> occurrenceData = (Map<String, Object>) faultData.get("occurrence");
                Occurrence occurrence = new Occurrence(
                        (String) occurrenceData.get("timing"),
                        (String) occurrenceData.get("target"),
                        (Map<String, String>) occurrenceData.get("location")
                );

                // Create a new Fault object with the parsed fault data
                Fault f = new Fault(
                        (String) faultData.get("type"),
                        (String) faultData.get("config"),
                        occurrence
                        );
                // Set the fault and trigger on the ConditionalEvent object
                ce.setFault(f);
            }

            if (triggerData != null) {
                // Create a new Trigger object with the parsed trigger data
                Trigger t = new Trigger((String) triggerData.get("type"), (Map<String, String>) triggerData.get("arguments"));

                ce.setTrigger(t);
            }

            // Add the ConditionalEvent to the FailurePlan
            fp.addEvent(ce);
        }

        return fp;
    }

    public static FailurePlan fromYamlFile(String filePath) {
        File planFile = new File(filePath);
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(planFile);
        } catch (FileNotFoundException e) {
            Logger.getLogger(PlanParser.class.getSimpleName()).warning("Failure plan file not found");
            return null;
        }
        return fromInputStream(inputStream);
    }

}

