package sae.sauvgarde.client.controller;

import sae.sauvgarde.client.Client;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sae.sauvgarde.client.model.OperationRequest;

@RestController
public class OperationController {

    @PostMapping("/performOperation")
    public String performOperation(@RequestBody OperationRequest request) {
        try {
            return Client.performOperation(
                    request.getFolderPath(),
                    request.getOperation(),
                    request.isUseZip() ? "yes" : "no"
            );
        } catch (Exception e) {
            return "Error occurred: " + e.getMessage();
        }
    }
}
