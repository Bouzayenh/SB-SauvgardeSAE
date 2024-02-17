package sae.sauvgarde.controller;

import sae.sauvgarde.client.Client;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sae.sauvgarde.model.OperationRequest;

@RestController
public class OperationController {

    @PostMapping("/performOperation")
    public String performOperation(@RequestBody OperationRequest request) {
        try {
            // Assuming the 'performOperation' method in Client class has been created as described below
            return Client.performOperation(request.getFolderPath(), request.getOperation(), request.isUseZip());
        } catch (Exception e) {
            // Handle exceptions
            return "Error occurred: " + e.getMessage();
        }
    }
}
