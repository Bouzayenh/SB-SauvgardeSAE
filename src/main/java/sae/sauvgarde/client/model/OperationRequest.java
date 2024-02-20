package sae.sauvgarde.client.model;

public class OperationRequest {
    private String folderPath;
    private String operation;
    private boolean useZip;

    // Constructors, getters, and setters
    public OperationRequest() {}

    public OperationRequest(String folderPath, String operation, boolean useZip) {
        this.folderPath = folderPath;
        this.operation = operation;
        this.useZip = useZip;
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
}
