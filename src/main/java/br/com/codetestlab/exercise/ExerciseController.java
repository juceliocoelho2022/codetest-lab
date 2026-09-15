package br.com.codetestlab.exercise;

import br.com.codetestlab.exercise.dto.CreateExerciseRequest;
import br.com.codetestlab.exercise.dto.ExerciseResponse;
import br.com.codetestlab.exercise.dto.SourceSubmissionRequest;
import br.com.codetestlab.exercise.dto.SubmissionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/exercises")
public class ExerciseController {
    private final ExerciseService service;

    public ExerciseController(ExerciseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExerciseResponse> create(@Valid @RequestBody CreateExerciseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public List<ExerciseResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ExerciseResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/submissions/source")
    public ResponseEntity<SubmissionResponse> submitSource(@PathVariable Long id,
                                                           @Valid @RequestBody SourceSubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submitSource(id, request));
    }

    @PostMapping(path = "/{id}/submissions/zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> submitZip(@PathVariable Long id,
                                                        @RequestParam String studentName,
                                                        @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.submitZip(id, studentName, file.getBytes()));
    }

    @GetMapping("/{id}/submissions")
    public List<SubmissionResponse> submissions(@PathVariable Long id) {
        return service.listSubmissions(id);
    }
}
