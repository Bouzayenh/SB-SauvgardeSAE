package sae.sauvgarde.client.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sae.sauvgarde.client.Client;
import sae.sauvgarde.client.model.OperationRequest;

import java.util.List;
import java.util.Set;

@RestController
public class OperationController {

    @PostMapping("/performOperation")
    public String performOperation(@RequestBody OperationRequest request) {
        try {
            OidcUser user = (OidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            request.setUsername(user.getName()); // Set the username from the OIDC user
            Set<String> extensions = request.getExtensions();
            return Client.performOperation(
                    request.getFolderPath(),
                    request.getOperation(),
                    request.isUseZip() ? "yes" : "no",
                    request.getUsername() ,// Pass the username to the method
                    extensions
            );
        } catch (Exception e) {
            return "Error occurred: " + e.getMessage();
        }
    }
}
