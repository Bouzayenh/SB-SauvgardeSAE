package sae.sauvgarde.client.model;

import java.util.List;
import java.util.Set;

public class OperationRequest {
    private String folderPath;
    private String operation;
    private boolean useZip;
    private String username;

    private Set<String> extensions;

    public OperationRequest() {}

    public OperationRequest(String folderPath, String operation, boolean useZip, String username) { // Update constructor
        this.folderPath = folderPath;
        this.operation = operation;
        this.useZip = useZip;
        this.username = username; // Set username
    }


    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public boolean isUseZip() {
        return useZip;
    }

    public void setUseZip(boolean useZip) {
        this.useZip = useZip;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(Set<String> extensions) {
        this.extensions = extensions;
    }
}
