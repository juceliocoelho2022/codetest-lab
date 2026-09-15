const $ = (id) => document.getElementById(id);
let selectedExercise = null;

const THEME_KEY = 'codetest-theme';
const themeToggle = $('themeToggle');
const themeIcon = $('themeIcon');
const themeLabel = $('themeLabel');
const runPersonalButton = $('runPersonal');
const executionProfile = $('executionProfile');
const profileHelp = $('profileHelp');

const PROFILE_HELP = {
  JUNIT5: 'JUnit 5 executa os testes sem bibliotecas adicionais de mock ou cobertura.',
  JUNIT5_MOCKITO: 'JUnit 5 executa testes; Mockito habilita mocks de dependências.',
  JUNIT5_JACOCO: 'JUnit 5 executa testes; JaCoCo mede a cobertura do código.',
  JUNIT5_MOCKITO_JACOCO: 'JUnit 5 + mocks com Mockito + cobertura de código com JaCoCo.'
};

function applyTheme(theme, persist = false) {
  const nextTheme = theme === 'light' ? 'light' : 'dark';
  document.documentElement.dataset.theme = nextTheme;
  const isDark = nextTheme === 'dark';
  themeIcon.textContent = isDark ? '🌙' : '☀️';
  themeLabel.textContent = isDark ? 'Escuro' : 'Claro';
  themeToggle.setAttribute(
    'aria-label',
    isDark ? 'Tema atual escuro. Ativar tema claro' : 'Tema atual claro. Ativar tema escuro'
  );
  themeToggle.setAttribute(
    'title',
    isDark ? 'Tema atual: escuro. Clique para usar claro.' : 'Tema atual: claro. Clique para usar escuro.'
  );
  if (persist) localStorage.setItem(THEME_KEY, nextTheme);
}

applyTheme(document.documentElement.dataset.theme);

themeToggle.addEventListener('click', () => {
  const current = document.documentElement.dataset.theme;
  applyTheme(current === 'dark' ? 'light' : 'dark', true);
});

function updateProfileHelp() {
  profileHelp.textContent = PROFILE_HELP[executionProfile.value] || PROFILE_HELP.JUNIT5_MOCKITO;
}

executionProfile.addEventListener('change', updateProfileHelp);
updateProfileHelp();

for (const tab of document.querySelectorAll('.tab')) {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    tab.classList.add('active');
    $(tab.dataset.tab).classList.add('active');
  });
}

async function api(url, options = {}) {
  const response = await fetch(url, options);
  const contentType = response.headers.get('content-type') || '';
  const body = contentType.includes('application/json') ? await response.json() : await response.text();
  if (!response.ok) {
    const message = typeof body === 'object' ? (body.message || JSON.stringify(body)) : body;
    throw new Error(message || `HTTP ${response.status}`);
  }
  return body;
}

function parseExecutionDetails(output = '') {
  const details = {};
  const assertion = output.match(/expected:\s*<(.+?)>\s*but was:\s*<(.+?)>/i);
  if (assertion) {
    details.expected = assertion[1];
    details.actual = assertion[2];
  }

  const failureLocation = output.match(/\[ERROR\]\s+([A-Za-z0-9_.$]+):(\d+)\s+expected:/);
  if (failureLocation) {
    details.test = failureLocation[1];
    details.line = failureLocation[2];
  }

  const compile = output.match(/\[ERROR\]\s+(.+?\.java):\[(\d+),(\d+)\]\s+(.+)/);
  if (compile) {
    details.file = compile[1].split(/[\\/]/).pop();
    details.line = compile[2];
    details.column = compile[3];
    details.compileMessage = compile[4].trim();
  }

  return details;
}

function formatDuration(durationMs) {
  const value = Number(durationMs) || 0;
  if (value < 1000) return `${Math.max(0, Math.round(value))} ms`;
  return `${(value / 1000).toLocaleString('pt-BR', {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1
  })} s`;
}

function resultPresentation(result) {
  switch (result.status) {
    case 'PASSED':
      return {
        icon: '✓',
        title: 'TODOS OS TESTES PASSARAM',
        tone: 'passed',
        message: 'Todos os testes foram concluídos com sucesso.'
      };
    case 'FAILED':
      return {
        icon: '×',
        title: result.testsFailed === 1 ? 'TESTE FALHOU' : 'TESTES FALHARAM',
        tone: 'failed',
        message: 'O código compilou, mas pelo menos um teste encontrou um resultado incorreto.'
      };
    case 'COMPILE_ERROR':
      return {
        icon: '!',
        title: 'ERRO DE COMPILAÇÃO',
        tone: 'compile-error',
        message: 'O código Java não pôde ser compilado.'
      };
    case 'TIMEOUT':
      return {
        icon: '⏱',
        title: 'TEMPO LIMITE EXCEDIDO',
        tone: 'timeout',
        message: 'A execução ultrapassou o tempo limite permitido.'
      };
    case 'INFRASTRUCTURE_ERROR':
      return {
        icon: '!',
        title: 'ERRO DE INFRAESTRUTURA',
        tone: 'infrastructure-error',
        message: 'O ambiente de execução encontrou um problema de infraestrutura.'
      };
    default:
      return {
        icon: '•',
        title: result.status || 'RESULTADO',
        tone: 'unknown',
        message: 'A execução foi concluída. Consulte os detalhes abaixo.'
      };
  }
}

function appendMetric(container, label, value, modifier = '') {
  const metric = document.createElement('div');
  metric.className = `result-metric ${modifier}`.trim();

  const metricValue = document.createElement('strong');
  metricValue.textContent = value;

  const metricLabel = document.createElement('span');
  metricLabel.textContent = label;

  metric.append(metricValue, metricLabel);
  container.appendChild(metric);
}

function appendTestMetrics(container, result) {
  if (result.status !== 'COMPILE_ERROR' && result.testsRun > 0) {
    const metrics = document.createElement('div');
    metrics.className = 'result-metrics';
    appendMetric(metrics, result.testsRun === 1 ? 'executado' : 'executados', result.testsRun);
    appendMetric(metrics, result.testsPassed === 1 ? 'aprovado' : 'aprovados', result.testsPassed, 'success');
    appendMetric(metrics, result.testsFailed === 1 ? 'falha' : 'falhas', result.testsFailed,
      result.testsFailed > 0 ? 'danger' : '');
    if (result.testsSkipped > 0) {
      appendMetric(metrics, result.testsSkipped === 1 ? 'ignorado' : 'ignorados', result.testsSkipped);
    }
    container.appendChild(metrics);
  }
}

function appendFriendlyDetails(container, result, details) {
  const rows = [];

  if (result.status === 'FAILED') {
    if (details.expected !== undefined) rows.push(['Esperado', details.expected]);
    if (details.actual !== undefined) rows.push(['Obtido', details.actual]);
    if (details.test) rows.push(['Teste', details.test]);
    if (details.line) rows.push(['Linha', details.line]);
  }

  if (result.status === 'COMPILE_ERROR') {
    if (details.file) rows.push(['Arquivo', details.file]);
    if (details.line) rows.push(['Linha', details.line]);
    if (details.column) rows.push(['Coluna', details.column]);
    if (details.compileMessage) rows.push(['Problema', details.compileMessage]);
  }

  if (rows.length === 0) return;

  const list = document.createElement('div');
  list.className = 'diagnostic-list';

  for (const [label, value] of rows) {
    const row = document.createElement('div');
    row.className = 'diagnostic-row';

    const key = document.createElement('span');
    key.className = 'diagnostic-key';
    key.textContent = label;

    const val = document.createElement('strong');
    val.className = 'diagnostic-value';
    val.textContent = value;

    row.append(key, val);
    list.appendChild(row);
  }

  container.appendChild(list);
}

function appendGuidance(container, result, details) {
  if (result.status !== 'COMPILE_ERROR') return;

  const guidance = document.createElement('div');
  guidance.className = 'result-guidance';
  guidance.textContent = details.line
    ? `Verifique a instrução próxima à linha ${details.line}.`
    : 'Revise a sintaxe Java indicada na mensagem do compilador.';
  container.appendChild(guidance);
}

function coverageValue(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return null;
  return Math.min(100, Math.max(0, Number(value)));
}

function formatCoverage(value) {
  const normalized = coverageValue(value);
  if (normalized === null) return '—';
  const rounded = Math.round(normalized * 10) / 10;
  return Number.isInteger(rounded) ? `${rounded}%` : `${rounded.toLocaleString('pt-BR')}%`;
}

function appendCoverage(container, coverage) {
  if (!coverage) return;

  const definitions = [
    ['Linhas', coverage.linePercent],
    ['Métodos', coverage.methodPercent],
    ['Branches', coverage.branchPercent],
    ['Classes', coverage.classPercent]
  ];

  if (!definitions.some(([, value]) => coverageValue(value) !== null)) return;

  const section = document.createElement('section');
  section.className = 'coverage-section';

  const heading = document.createElement('div');
  heading.className = 'coverage-heading';
  heading.innerHTML = '<div><span class="coverage-kicker">JACOCO</span><h3>Cobertura de código</h3></div>';

  const note = document.createElement('p');
  note.className = 'coverage-note';
  note.textContent = 'Cobertura indica o código executado pelos testes; não garante, sozinha, a qualidade dos testes.';
  heading.appendChild(note);

  const grid = document.createElement('div');
  grid.className = 'coverage-grid';

  for (const [label, rawValue] of definitions) {
    const value = coverageValue(rawValue);
    const item = document.createElement('div');
    item.className = 'coverage-item';

    const meta = document.createElement('div');
    meta.className = 'coverage-meta';
    const name = document.createElement('span');
    name.textContent = label;
    const percent = document.createElement('strong');
    percent.textContent = formatCoverage(rawValue);
    meta.append(name, percent);

    const track = document.createElement('div');
    track.className = 'coverage-bar';
    track.setAttribute('role', 'progressbar');
    track.setAttribute('aria-label', `Cobertura de ${label}`);
    track.setAttribute('aria-valuemin', '0');
    track.setAttribute('aria-valuemax', '100');
    if (value !== null) track.setAttribute('aria-valuenow', String(value));

    const fill = document.createElement('span');
    fill.className = 'coverage-fill';
    fill.style.width = value === null ? '0%' : `${value}%`;
    track.appendChild(fill);

    item.append(meta, track);
    grid.appendChild(item);
  }

  section.append(heading, grid);
  container.appendChild(section);
}

function appendTechnicalDetails(container, output) {
  const normalized = (output || '').trim();
  if (!normalized) return;

  const details = document.createElement('details');
  details.className = 'result-details';

  const summary = document.createElement('summary');
  summary.textContent = 'Ver detalhes técnicos';

  const pre = document.createElement('pre');
  pre.className = 'execution-output';
  pre.textContent = normalized;

  details.append(summary, pre);
  container.appendChild(details);
}

function renderExecution(target, result) {
  const parsed = parseExecutionDetails(result.output || '');
  const presentation = resultPresentation(result);

  target.className = `result result-card ${presentation.tone}`;
  target.innerHTML = '';

  const top = document.createElement('div');
  top.className = 'result-top';

  const statusBlock = document.createElement('div');
  statusBlock.className = 'result-status-block';

  const icon = document.createElement('span');
  icon.className = 'result-status-icon';
  icon.textContent = presentation.icon;
  icon.setAttribute('aria-hidden', 'true');

  const titleGroup = document.createElement('div');
  const code = document.createElement('span');
  code.className = 'result-status-code';
  code.textContent = result.status;
  const title = document.createElement('h3');
  title.className = 'result-status-title';
  title.textContent = presentation.title;
  titleGroup.append(code, title);

  statusBlock.append(icon, titleGroup);

  const duration = document.createElement('div');
  duration.className = 'result-duration';
  duration.innerHTML = `<span>Duração</span><strong>${formatDuration(result.durationMs)}</strong>`;

  top.append(statusBlock, duration);

  const message = document.createElement('p');
  message.className = 'result-message';
  if (result.status === 'FAILED' && parsed.expected !== undefined && parsed.actual !== undefined) {
    message.textContent = 'O teste executou, mas o resultado obtido foi diferente do esperado.';
  } else if (result.status === 'COMPILE_ERROR' && parsed.compileMessage) {
    message.textContent = `O código não compilou: ${parsed.compileMessage}`;
  } else {
    message.textContent = presentation.message;
  }

  target.append(top, message);
  appendTestMetrics(target, result);
  appendFriendlyDetails(target, result, parsed);
  appendGuidance(target, result, parsed);

  if (result.status !== 'COMPILE_ERROR' && result.coverage) {
    appendCoverage(target, result.coverage);
  }

  appendTechnicalDetails(target, result.output);
}

function renderError(target, error) {
  target.className = 'result result-card infrastructure-error';
  target.innerHTML = '';

  const top = document.createElement('div');
  top.className = 'result-top';
  top.innerHTML = `
    <div class="result-status-block">
      <span class="result-status-icon" aria-hidden="true">!</span>
      <div>
        <span class="result-status-code">ERRO</span>
        <h3 class="result-status-title">NÃO FOI POSSÍVEL EXECUTAR</h3>
      </div>
    </div>`;

  const message = document.createElement('p');
  message.className = 'result-message';
  message.textContent = 'Não foi possível concluir a solicitação.';

  target.append(top, message);
  appendTechnicalDetails(target, error.message);
}

function resetPersonalResult() {
  const target = $('personalResult');
  target.className = 'result empty';
  target.textContent = 'O resultado aparecerá aqui.';
}

async function runPersonalTests() {
  const target = $('personalResult');
  target.className = 'result empty running';
  target.textContent = 'Executando testes...';
  runPersonalButton.disabled = true;
  runPersonalButton.textContent = 'Executando...';

  try {
    const result = await api('/api/v1/playground/run', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({
        className: $('personalClassName').value,
        sourceCode: $('personalSource').value,
        testCode: $('personalTest').value,
        executionProfile: $('executionProfile').value
      })
    });
    renderExecution(target, result);
  } catch (error) {
    renderError(target, error);
  } finally {
    runPersonalButton.disabled = false;
    runPersonalButton.textContent = '▶ Executar testes';
  }
}

runPersonalButton.addEventListener('click', runPersonalTests);
$('clearPersonalResult').addEventListener('click', resetPersonalResult);

document.addEventListener('keydown', (event) => {
  const personalIsActive = $('personal').classList.contains('active');
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter' && personalIsActive) {
    event.preventDefault();
    if (!runPersonalButton.disabled) runPersonalButton.click();
  }
});

$('createExercise').addEventListener('click', async () => {
  try {
    const created = await api('/api/v1/exercises', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({
        title: $('exerciseTitle').value,
        description: $('exerciseDescription').value,
        className: $('exerciseClassName').value,
        hiddenTestCode: $('exerciseHiddenTest').value
      })
    });
    await loadExercises(created.id);
  } catch (error) { alert(error.message); }
});

$('refreshExercises').addEventListener('click', () => loadExercises());
$('refreshHistory').addEventListener('click', () => loadHistory());

async function loadExercises(selectId = null) {
  const list = $('exerciseList');
  try {
    const exercises = await api('/api/v1/exercises');
    list.innerHTML = '';
    list.classList.toggle('empty', exercises.length === 0);
    if (exercises.length === 0) {
      list.textContent = 'Nenhum exercício cadastrado.';
      return;
    }
    for (const exercise of exercises) {
      const item = document.createElement('div');
      item.className = 'exercise-item';
      item.dataset.id = exercise.id;
      item.innerHTML = `<strong></strong><span></span>`;
      item.querySelector('strong').textContent = exercise.title;
      item.querySelector('span').textContent = `${exercise.className} • ${exercise.description}`;
      item.addEventListener('click', () => selectExercise(exercise, item));
      list.appendChild(item);
      if (selectId === exercise.id) selectExercise(exercise, item);
    }
  } catch (error) { list.textContent = error.message; }
}

function selectExercise(exercise, item) {
  selectedExercise = exercise;
  document.querySelectorAll('.exercise-item').forEach(el => el.classList.remove('selected'));
  item.classList.add('selected');
  $('selectedExerciseTitle').textContent = exercise.title;
  $('selectedExerciseDescription').textContent = `${exercise.description} Classe esperada: ${exercise.className}.`;
  $('submissionArea').classList.remove('hidden');
  $('historyCard').classList.remove('hidden');
  loadHistory();
}

$('submitSource').addEventListener('click', async () => {
  if (!selectedExercise) return;
  const target = $('submissionResult');
  target.className = 'result empty running';
  target.textContent = 'Executando submissão...';
  try {
    const result = await api(`/api/v1/exercises/${selectedExercise.id}/submissions/source`, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({studentName: $('studentName').value, sourceCode: $('studentSource').value})
    });
    renderExecution(target, result);
    await loadHistory();
  } catch (error) { renderError(target, error); }
});

$('submitZip').addEventListener('click', async () => {
  if (!selectedExercise) return;
  const file = $('zipFile').files[0];
  if (!file) return alert('Selecione um arquivo .zip.');
  const form = new FormData();
  form.append('studentName', $('studentName').value);
  form.append('file', file);
  const target = $('submissionResult');
  target.className = 'result empty running';
  target.textContent = 'Enviando e executando ZIP...';
  try {
    const result = await api(`/api/v1/exercises/${selectedExercise.id}/submissions/zip`, {method: 'POST', body: form});
    renderExecution(target, result);
    await loadHistory();
  } catch (error) { renderError(target, error); }
});

async function loadHistory() {
  if (!selectedExercise) return;
  const container = $('submissionHistory');
  try {
    const items = await api(`/api/v1/exercises/${selectedExercise.id}/submissions`);
    container.innerHTML = '';
    container.classList.toggle('empty', items.length === 0);
    if (items.length === 0) {
      container.textContent = 'Nenhuma submissão para este exercício.';
      return;
    }
    const table = document.createElement('table');
    table.innerHTML = '<thead><tr><th>Aluno</th><th>Tipo</th><th>Status</th><th>Testes</th><th>Duração</th><th>Data</th></tr></thead><tbody></tbody>';
    const body = table.querySelector('tbody');
    for (const item of items) {
      const row = document.createElement('tr');
      row.innerHTML = '<td></td><td></td><td></td><td></td><td></td><td></td>';
      const cells = row.querySelectorAll('td');
      cells[0].textContent = item.studentName;
      cells[1].textContent = item.submissionType;
      cells[2].innerHTML = `<span class="status ${item.status}">${item.status}</span>`;
      cells[3].textContent = `${item.testsPassed}/${item.testsRun}`;
      cells[4].textContent = formatDuration(item.durationMs);
      cells[5].textContent = new Date(item.createdAt).toLocaleString('pt-BR');
      body.appendChild(row);
    }
    container.appendChild(table);
  } catch (error) { container.textContent = error.message; }
}

loadExercises();
