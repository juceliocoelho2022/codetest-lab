package br.com.codetestlab.web;

import br.com.codetestlab.execution.CodeExecutor;
import br.com.codetestlab.execution.ExecutionProfile;
import br.com.codetestlab.execution.ExecutionRequest;
import br.com.codetestlab.execution.ExecutionResult;
import br.com.codetestlab.execution.JavaSourceFile;
import br.com.codetestlab.web.dto.PlaygroundRunRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        boolean hasLegacySource = request.sourceCode() != null && !request.sourceCode().isBlank();
        boolean hasSourceFiles = request.sourceFiles() != null && !request.sourceFiles().isEmpty();

        if (hasLegacySource == hasSourceFiles) {
            throw new IllegalArgumentException(
                    "Informe exatamente uma origem: sourceCode ou sourceFiles.");
        }

        ExecutionRequest executionRequest;
        if (hasLegacySource) {
            executionRequest = ExecutionRequest.source(
                    request.className(),
                    request.sourceCode(),
                    request.testCode(),
                    profile);
        } else {
            List<JavaSourceFile> sourceFiles = request.sourceFiles().stream()
                    .map(file -> new JavaSourceFile(file.fileName(), file.content()))
                    .toList();
            executionRequest = ExecutionRequest.sourceFiles(
                    request.className(),
                    sourceFiles,
                    request.testCode(),
                    profile);
        }

        return ResponseEntity.ok(codeExecutor.execute(executionRequest));
    }
}
