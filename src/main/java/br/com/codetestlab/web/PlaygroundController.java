package br.com.codetestlab.web;

import br.com.codetestlab.execution.CodeExecutor;
import br.com.codetestlab.execution.ExecutionProfile;
import br.com.codetestlab.execution.ExecutionRequest;
import br.com.codetestlab.execution.ExecutionResult;
import br.com.codetestlab.web.dto.PlaygroundRunRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/playground")
public class PlaygroundController {
    private final CodeExecutor codeExecutor;

    public PlaygroundController(CodeExecutor codeExecutor) {
        this.codeExecutor = codeExecutor;
    }

    @PostMapping("/run")
    public ResponseEntity<ExecutionResult> run(@Valid @RequestBody PlaygroundRunRequest request) {
        ExecutionProfile profile = ExecutionProfile.fromNullable(request.executionProfile());
        return ResponseEntity.ok(codeExecutor.execute(ExecutionRequest.source(
                request.className(),
                request.sourceCode(),
                request.testCode(),
                profile)));
    }
}
