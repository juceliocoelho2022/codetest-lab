package br.com.codetestlab.exercise;

import br.com.codetestlab.config.ExecutionProperties;
import br.com.codetestlab.execution.CodeExecutor;
import br.com.codetestlab.execution.ExecutionResult;
import br.com.codetestlab.execution.ExecutionStatus;
import br.com.codetestlab.exercise.dto.CreateExerciseRequest;
import br.com.codetestlab.exercise.dto.SourceSubmissionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {
    @Mock ExerciseRepository exerciseRepository;
    @Mock SubmissionRepository submissionRepository;
    @Mock CodeExecutor codeExecutor;

    private ExerciseService service;

    @BeforeEach
    void setUp() {
        var properties = new ExecutionProperties("runner", 20, 20_000, 100_000,
                1_048_576, 200, 2_000_000);
        service = new ExerciseService(exerciseRepository, submissionRepository, codeExecutor, properties);
    }

    @Test
    void shouldCreateExerciseWithoutExposingHiddenTest() {
        var entity = new Exercise("Soma", "Some dois números", "Calculadora", "class HiddenTest {}");
        when(exerciseRepository.save(any())).thenReturn(entity);

        var response = service.create(new CreateExerciseRequest(
                "Soma", "Some dois números", "Calculadora", "class HiddenTest {}"));

        assertEquals("Soma", response.title());
        verify(exerciseRepository).save(any());
    }

    @Test
    void shouldExecuteAndPersistSourceSubmission() {
        var exercise = new Exercise("Soma", "Descrição", "Calculadora", "class CalculadoraTest {}");
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(codeExecutor.execute(any())).thenReturn(new ExecutionResult(
                ExecutionStatus.PASSED, 2, 2, 0, 0, 25, "ok"));
        when(submissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.submitSource(1L, new SourceSubmissionRequest(
                "Aluno", "public class Calculadora {}"));

        assertEquals(ExecutionStatus.PASSED, response.status());
        verify(codeExecutor).execute(any());
        verify(submissionRepository).save(any());
    }
}
