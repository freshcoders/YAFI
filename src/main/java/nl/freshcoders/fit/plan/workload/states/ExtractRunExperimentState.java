package nl.freshcoders.fit.plan.workload.states;

import nl.freshcoders.fit.Orchestrator;
import nl.freshcoders.fit.plan.FailurePlan;
import nl.freshcoders.fit.plan.parser.PlanParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExtractRunExperimentState implements WorkloadState {
    private boolean done = false;
    @Override
    public boolean isReady() {
        return done;
    }
    @Override
    public void execute(Orchestrator orchestrator) {
        orchestrator.getFailurePlanRunner().writeOut();
        orchestrator.getFailurePlanRunner().cyclePlanState();


        String runId = System.getProperty("run_id");
        String runDir = ensurePrefix(runId);
        Path genPath = Path.of(System.getProperty("user.dir") + "/generatedplans/" + runDir);
        if (Files.notExists(genPath)) {
            System.out.println("Specified run ID not found in plan generations:\n" + genPath);
            System.exit(0);
        }
        String fileExtension = ".yml";

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(genPath,
                path -> path.toString().endsWith(fileExtension))) {
            for (Path path : stream) {
                try (InputStream inputStream = new FileInputStream(path.toFile())) {
                    FailurePlan failurePlan = PlanParser.fromInputStream(inputStream);
                    String planFilename = path.getFileName().toString();
                    int fileExtPos = planFilename.indexOf(fileExtension);
                    failurePlan.reference = planFilename.substring(0, fileExtPos);
                    orchestrator.generations.add(failurePlan);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        done = true;
    }

    public static String ensurePrefix(String id) {
        String prefix = "run_";
        String idStr = String.valueOf(id);
        if (!idStr.startsWith(prefix)) {
            idStr = prefix + idStr;
        }
        return idStr;
    }

}
