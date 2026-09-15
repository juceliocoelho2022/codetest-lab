package br.com.codetestlab.execution;

public record CoverageResult(
        Double linePercent,
        Double methodPercent,
        Double branchPercent,
        Double classPercent
) {
}
