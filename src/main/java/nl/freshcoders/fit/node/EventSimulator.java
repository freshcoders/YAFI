package nl.freshcoders.fit.node;

import nl.freshcoders.fit.connection.ConnectionPool;
import nl.freshcoders.fit.plan.runner.FailurePlanRunner;
import nl.freshcoders.fit.target.RemoteTarget;

import java.util.Scanner;

public class EventSimulator implements Runnable {

    FailurePlanRunner failurePlanRunner;

    public EventSimulator(FailurePlanRunner fpr) {
        failurePlanRunner = fpr;
    }

    public boolean awaitingInput = false;

    public static Scanner input;

    @Override
    public void run() {
        // Get user input for whether to send a trigger
        if (!awaitingInput) {
            System.out.println("Simulating application, enter command: (t, i, r, x, q, c, ?)");
            awaitingInput = true;
        }

        if (input == null) {
            input = new Scanner(System.in);
//            return;
        }

        if (input.hasNext()) {
            awaitingInput = false;
            String userInput = input.nextLine();
            if (userInput.equals("q")) {
                ConnectionPool.getAgentConnection(new RemoteTarget("localhost", 13013)).run("exit");
                ConnectionPool.getAgentConnection(new RemoteTarget("localhost", 13014)).run("exit");

                System.exit(0);
            }
            if (userInput.equals("?")) {
                System.out.println("i: (re) install byteman into the SUT");
                System.out.println("b: inject a BM rule by name");
                System.out.println("t: trigger a fault");
                System.out.println("c: clear (remove all rules)");
                System.out.println("x: execute simple functional test");
                System.out.println("r: re-establish socket connections");
                System.out.println("q: quit");
                System.out.println("?: show this help menu");
            }
            if (userInput.equals("r")) {
                ConnectionPool.closeAll();
                failurePlanRunner.setup();
            }
            if (userInput.equals("i")) {
                // XXX: replace with options..
                ConnectionPool.getAgentConnection(new RemoteTarget("localhost", 13013)).run("byteman:attach");
            }

            if (userInput.equals("t")) {
                // disabled for now
//                Scanner in = new Scanner(System.in);
//                String src = in.next();
//                Target target = new LocalTarget(...);
//                failurePlanRunner.appendQueue(new TriggerValue(src, "clock", "11"));
                // Get user input for the trigger type and argument
//                    System.out.println("Enter the trigger type:");
//                    String triggerType = input.nextLine();
//                    System.out.println("Enter the trigger value:");
//                    String triggerVal = input.nextLine();
//
//                    System.out.println("Sending trigger...");
//                    fpr.appendQueue(new TriggerValue("trace", triggerVal));
            }
            if (userInput.equals("c")) {
                ConnectionPool.getAgentConnection(new RemoteTarget("localhost", 13013)).run("byteman:clear");
            }
        }
    }
}
