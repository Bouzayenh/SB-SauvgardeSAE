package sae.sauvgarde.client;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

@Component
public class DockerBackupScheduler {

    @Scheduled(fixedRate = 600000) // 600000 ms = 10 minutes
    public void backupDockerVolumes() {
        try {
            String dockerVolumesPath = "C:\\Users\\bh13h\\Desktop\\test";
            Set<String> allowedExtensions = Collections.emptySet();

            Client.performOperation(dockerVolumesPath, "backup", "yes", "docker", allowedExtensions);

            System.out.println("Docker volumes backed up successfully.");
        } catch (Exception e) {
            System.err.println("Failed to backup Docker volumes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
