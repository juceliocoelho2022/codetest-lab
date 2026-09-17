package br.com.codetestlab.web.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaygroundRunRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldRejectNullElementInsideSourceFiles() {
        PlaygroundRunRequest request = new PlaygroundRunRequest(
                "PedidoService",
                null,
                Collections.singletonList(null),
                "class PedidoServiceTest {}",
                "JUNIT5");

        var violations = validator.validate(request);

        assertTrue(
                violations.stream().anyMatch(violation ->
                        violation.getConstraintDescriptor()
                                .getAnnotation()
                                .annotationType()
                                .equals(NotNull.class)),
                "sourceFiles must reject null list elements with @NotNull");
    }
}
