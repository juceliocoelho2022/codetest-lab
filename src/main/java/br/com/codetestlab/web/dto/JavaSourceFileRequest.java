package br.com.codetestlab.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JavaSourceFileRequest(
        @NotBlank @Size(max = 190) String fileName,
        @NotBlank @Size(max = 100_000) String content
) {}
