$ErrorActionPreference = "Stop"

Write-Host "=== CodeTest Lab ===" -ForegroundColor Cyan

function Require-Command($name) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "Comando '$name' não encontrado. Instale/configure antes de continuar."
    }
}

Require-Command "java"
Require-Command "mvn"
Require-Command "docker"

Write-Host "[1/4] Verificando Docker..." -ForegroundColor Yellow
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Docker Desktop não está ativo. Inicie o Docker Desktop e execute novamente."
}

Write-Host "[2/4] Atualizando imagem do runner..." -ForegroundColor Yellow
# O Docker reutiliza o cache quando runner/Dockerfile e runner/pom.xml nao mudaram.
# Se o runner for alterado, a imagem sera reconstruida automaticamente.
docker build -t codetest-lab-runner:latest .\runner
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao criar/atualizar a imagem codetest-lab-runner:latest."
}

Write-Host "[3/4] Executando testes do projeto..." -ForegroundColor Yellow
mvn test
if ($LASTEXITCODE -ne 0) {
    throw "Os testes do projeto falharam."
}

Write-Host "[4/4] Iniciando aplicação em http://localhost:8080" -ForegroundColor Green
mvn spring-boot:run
