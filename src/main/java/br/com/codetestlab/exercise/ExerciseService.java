package br.com.codetestlab.exercise;

import br.com.codetestlab.config.ExecutionProperties;
import br.com.codetestlab.execution.CodeExecutor;
import br.com.codetestlab.execution.ExecutionRequest;
import br.com.codetestlab.exercise.dto.CreateExerciseRequest;
import br.com.codetestlab.exercise.dto.ExerciseResponse;
import br.com.codetestlab.exercise.dto.SourceSubmissionRequest;
import br.com.codetestlab.exercise.dto.SubmissionResponse;
import br.com.codetestlab.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExerciseService {
    private final ExerciseRepository exerciseRepository;
    private final SubmissionRepository submissionRepository;
    private final CodeExecutor codeExecutor;
    private final ExecutionProperties properties;

    public ExerciseService(ExerciseRepository exerciseRepository,
                           SubmissionRepository submissionRepository,
                           CodeExecutor codeExecutor,
                           ExecutionProperties properties) {
        this.exerciseRepository = exerciseRepository;
        this.submissionRepository = submissionRepository;
        this.codeExecutor = codeExecutor;
        this.properties = properties;
    }

    @Transactional
    public ExerciseResponse create(CreateExerciseRequest request) {
        var exercise = new Exercise(
                request.title().trim(),
                request.description().trim(),
                request.className().trim(),
                request.hiddenTestCode());
        return toResponse(exerciseRepository.save(exercise));
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> list() {
        return exerciseRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ExerciseResponse get(Long id) {
        return toResponse(requireExercise(id));
    }

    @Transactional
    public SubmissionResponse submitSource(Long exerciseId, SourceSubmissionRequest request) {
        Exercise exercise = requireExercise(exerciseId);
        if (request.sourceCode().length() > properties.maxSourceChars()) {
            throw new IllegalArgumentException("Código-fonte excede o limite permitido.");
        }
        var result = codeExecutor.execute(ExecutionRequest.source(
                exercise.getClassName(), request.sourceCode(), exercise.getHiddenTestCode()));
        return toResponse(submissionRepository.save(
                new Submission(exercise, request.studentName().trim(), SubmissionType.SOURCE, result)));
    }

    @Transactional
    public SubmissionResponse submitZip(Long exerciseId, String studentName, byte[] zipBytes) {
        Exercise exercise = requireExercise(exerciseId);
        if (studentName == null || studentName.isBlank()) {
            throw new IllegalArgumentException("studentName é obrigatório.");
        }
        if (zipBytes == null || zipBytes.length == 0) {
            throw new IllegalArgumentException("Arquivo ZIP é obrigatório.");
        }
        if (zipBytes.length > properties.maxZipBytes()) {
            throw new IllegalArgumentException("ZIP excede o limite permitido.");
        }
        var result = codeExecutor.execute(ExecutionRequest.zip(
                exercise.getClassName(), zipBytes, exercise.getHiddenTestCode()));
        return toResponse(submissionRepository.save(
                new Submission(exercise, studentName.trim(), SubmissionType.ZIP, result)));
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listSubmissions(Long exerciseId) {
        requireExercise(exerciseId);
        return submissionRepository.findByExerciseIdOrderByCreatedAtDesc(exerciseId)
                .stream().map(this::toResponse).toList();
    }

    private Exercise requireExercise(Long id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercício não encontrado: " + id));
    }

    private ExerciseResponse toResponse(Exercise exercise) {
        return new ExerciseResponse(exercise.getId(), exercise.getTitle(), exercise.getDescription(),
                exercise.getClassName(), exercise.getCreatedAt());
    }

    private SubmissionResponse toResponse(Submission submission) {
        return new SubmissionResponse(
                submission.getId(), submission.getExercise().getId(), submission.getStudentName(),
                submission.getSubmissionType(), submission.getStatus(), submission.getTestsRun(),
                submission.getTestsPassed(), submission.getTestsFailed(), submission.getTestsSkipped(),
                submission.getDurationMs(), submission.getOutput(), submission.getCreatedAt());
    }
}
