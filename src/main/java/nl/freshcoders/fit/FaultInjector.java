package nl.freshcoders.fit;

import org.apache.log4j.PropertyConfigurator;

/**
 * Main entrypoint for perturbing a SUT from the command line.
 */
public class FaultInjector {

    public static void main(String[] args) {
        PropertyConfigurator.configure("src/main/resources/log4j.properties");

        String mode = "agent";
        if (args.length > 0)
            mode = args[0];

        // For now we only support a single parameter as an argument to indicate we run as orchestrator
        // otherwise, we assume the application is running as an agent
        if (mode.equals("--orchestrator") || mode.equals("-o")) {
            System.out.println("Running in orchestrator");
            Orchestrator orchestrator = Orchestrator.getInstance();
            orchestrator.addEventSimulator();
            orchestrator.run();
        } else {
            if (System.getProperty("datadir") == null) {
                throw new IllegalArgumentException("No -Ddatadir option found! Please configure it!");
            }
            System.out.println("Running in agent mode");
            Agent agent = Agent.getInstance();
            agent.run();
        }
    }
}