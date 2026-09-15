const $ = (id) => document.getElementById(id);
let selectedExercise = null;

const THEME_KEY = 'codetest-theme';
const themeToggle = $('themeToggle');
const themeIcon = $('themeIcon');
const themeLabel = $('themeLabel');
const runPersonalButton = $('runPersonal');

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

function friendlyMessage(result, details) {
  switch (result.status) {
    case 'PASSED':
      return 'Todos os testes foram concluídos com sucesso.';
    case 'FAILED':
      if (details.expected !== undefined && details.actual !== undefined) {
        return 'O teste executou, mas o resultado obtido foi diferente do esperado.';
      }
      return 'O código compilou, mas pelo menos um teste falhou.';
    case 'COMPILE_ERROR':
      return details.compileMessage
        ? `O código não compilou: ${details.compileMessage}`
        : 'O código Java contém um erro de compilação.';
    case 'TIMEOUT':
      return 'A execução ultrapassou o tempo limite permitido.';
    case 'INFRASTRUCTURE_ERROR':
      return 'O ambiente de execução encontrou um problema de infraestrutura.';
    default:
      return 'A execução foi concluída. Consulte os detalhes abaixo.';
  }
}

function appendMetric(container, value) {
  const metric = document.createElement('span');
  metric.className = 'metric';
  metric.textContent = value;
  container.appendChild(metric);
}

function appendFriendlyDetails(container, result, details) {
  const rows = [];
  if (details.expected !== undefined) rows.push(['Esperado', details.expected]);
  if (details.actual !== undefined) rows.push(['Obtido', details.actual]);
  if (details.test) rows.push(['Teste', details.test]);
  if (details.file) rows.push(['Arquivo', details.file]);
  if (details.line) rows.push(['Linha', details.line]);
  if (details.column) rows.push(['Coluna', details.column]);

  if (rows.length === 0) return;

  const grid = document.createElement('div');
  grid.className = 'friendly-grid';
  for (const [label, value] of rows) {
    const item = document.createElement('div');
    item.className = 'friendly-item';
    const key = document.createElement('span');
    key.className = 'friendly-key';
    key.textContent = label;
    const val = document.createElement('strong');
    val.textContent = value;
    item.append(key, val);
    grid.appendChild(item);
  }
  container.appendChild(grid);
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
  target.classList.remove('empty');
  target.innerHTML = '';

  const parsed = parseExecutionDetails(result.output || '');

  const header = document.createElement('div');
  header.className = 'result-header';

  const status = document.createElement('span');
  status.className = `status ${result.status}`;
  status.textContent = result.status;

  const summary = document.createElement('div');
  summary.className = 'execution-summary';
  appendMetric(summary, `${result.testsPassed}/${result.testsRun} testes`);
  if (result.testsFailed > 0) appendMetric(summary, `${result.testsFailed} falharam`);
  if (result.testsSkipped > 0) appendMetric(summary, `${result.testsSkipped} ignorados`);
  appendMetric(summary, `${result.durationMs} ms`);

  header.append(status, summary);

  const message = document.createElement('div');
  message.className = 'result-message';
  message.textContent = friendlyMessage(result, parsed);

  target.append(header, message);
  appendFriendlyDetails(target, result, parsed);
  appendTechnicalDetails(target, result.output);
}

function renderError(target, error) {
  target.classList.remove('empty');
  target.innerHTML = '';

  const status = document.createElement('span');
  status.className = 'status FAILED';
  status.textContent = 'ERRO';

  const message = document.createElement('div');
  message.className = 'result-message';
  message.textContent = 'Não foi possível concluir a solicitação.';

  const details = document.createElement('details');
  details.className = 'result-details';
  const summary = document.createElement('summary');
  summary.textContent = 'Ver detalhes técnicos';
  const pre = document.createElement('pre');
  pre.textContent = error.message;
  details.append(summary, pre);

  target.append(status, message, details);
}

function resetPersonalResult() {
  const target = $('personalResult');
  target.className = 'result empty';
  target.textContent = 'O resultado aparecerá aqui.';
}

async function runPersonalTests() {
  const target = $('personalResult');
  target.classList.add('empty');
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
        testCode: $('personalTest').value
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
      cells[4].textContent = `${item.durationMs} ms`;
      cells[5].textContent = new Date(item.createdAt).toLocaleString('pt-BR');
      body.appendChild(row);
    }
    container.appendChild(table);
  } catch (error) { container.textContent = error.message; }
}

loadExercises();