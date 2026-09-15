#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/target/core-smoke"
rm -rf "$OUT"
mkdir -p "$OUT"

javac -d "$OUT" \
  "$ROOT/src/main/java/br/com/codetestlab/execution/ExecutionStatus.java" \
  "$ROOT/src/main/java/br/com/codetestlab/execution/ExecutionResult.java" \
  "$ROOT/src/main/java/br/com/codetestlab/execution/ExecutionOutputParser.java" \
  "$ROOT/src/main/java/br/com/codetestlab/execution/DockerCommandBuilder.java" \
  "$ROOT/src/main/java/br/com/codetestlab/execution/SafeZipExtractor.java" \
  "$ROOT/src/main/java/br/com/codetestlab/execution/SurefireReportReader.java" \
  "$ROOT/tools/CoreSmokeTest.java"

java -cp "$OUT" CoreSmokeTest


grep -q 'mvn -q test' "$ROOT/runner/Dockerfile" || {
  echo "runner Dockerfile must warm Surefire cache with an actual test" >&2
  exit 1
}
