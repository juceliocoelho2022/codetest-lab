package br.com.codetestlab.web;

import br.com.codetestlab.execution.DockerHealthService;
import br.com.codetestlab.execution.RunnerHealth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final DockerHealthService dockerHealthService;

    public HealthController(DockerHealthService dockerHealthService) {
        this.dockerHealthService = dockerHealthService;
    }

    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "UP", "application", "CodeTest Lab");
    }

    @GetMapping("/runner")
    public RunnerHealth runnerHealth() {
        return dockerHealthService.check();
    }
}
