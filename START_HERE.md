# Comece por aqui

## IntelliJ IDEA

1. Extraia o ZIP.
2. Abra a pasta `codetest-lab` no IntelliJ.
3. Aguarde o Maven importar as dependências.
4. Confirme que o Project SDK está em **Java 21**.
5. Deixe o Docker Desktop aberto.

## Forma mais rápida no Windows

No terminal PowerShell da raiz do projeto:

```powershell
.\start.ps1
```

O script:

- verifica Java, Maven e Docker;
- cria `codetest-lab-runner:latest` se necessário;
- executa `mvn test`;
- inicia o Spring Boot.

Depois abra:

```text
http://localhost:8080
```

## Primeiro teste

A aba **Modo pessoal** já vem preenchida com uma `Calculadora` e um teste JUnit. Basta clicar em **Executar testes**.

Na aba **Professor / Aluno**, crie um exercício com teste oculto, selecione-o e envie uma solução colando o código ou usando um `.zip`.
