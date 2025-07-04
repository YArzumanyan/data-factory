package cz.cuni.mff.artifactrepo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class RootController {

    @Value("${springdoc.swagger-ui.path:/swagger-ui/index.html}")
    private String swaggerUiPath;

    /**
     * Handles requests to the root API endpoint.
     *
     * @return A redirect to the Swagger UI documentation.
     */
    @RequestMapping
    public String index() {
        return "redirect:" + swaggerUiPath;
    }

    @GetMapping("/health")
    public ResponseEntity<Boolean> health() {
        return ResponseEntity.ok(true);
    }
}

