package cz.cuni.mff.metadata_store.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class RootController {
    /**
     * Handles requests to the root API endpoint.
     *
     * @return A redirect to the Swagger UI documentation.
     */
    @RequestMapping
    public String index() {
        return "redirect:/swagger-ui/index.html";
    }

    @GetMapping("/alive")
    public ResponseEntity<Boolean> alive() {
        return ResponseEntity.ok(true);
    }
}

