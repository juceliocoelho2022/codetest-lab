const $ = (id) => document.getElementById(id);
let selectedExercise = null;

const THEME_KEY = 'codetest-theme';
const themeToggle = $('themeToggle');
const themeIcon = $('themeIcon');
const themeLabel = $('themeLabel');

function applyTheme(theme, persist = false) {
  const nextTheme = theme === 'light' ? 'light' : 'dark';
  document.documentElement.dataset.theme = nextTheme;
  const switchingToLight = nextTheme === 'dark';
  themeIcon.textContent = switchingToLight ? '☀️' : '🌙';
  themeLabel.textContent = switchingToLight ? 'Light' : 'Dark';
  themeToggle.setAttribute('aria-label', switchingToLight ? 'Ativar tema claro' : 'Ativar tema escuro');
  themeToggle.setAttribute('title', switchingToLight ? 'Ativar tema claro' : 'Ativar tema escuro');
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

function renderExecution(target, result) {
  target.classList.remove('empty');
  target.innerHTML = '';
  const status = document.createElement('div');
  status.className = `status ${result.status}`;
  status.textContent = result.status;
  const summary = document.createElement('div');
  summary.className = 'execution-summary';
  const metrics = [
    `Testes: ${result.testsRun}`,
    `Passaram: ${result.testsPassed}`,
    `Falharam: ${result.testsFailed}`,
    `Ignorados: ${result.testsSkipped}`,
    `${result.durationMs} ms`
  ];
  for (const value of metrics) {
    const metric = document.createElement('span');
    metric.className = 'metric';
    metric.textContent = value;
    summary.appendChild(metric);
  }
  const output = document.createElement('pre');
  output.className = 'execution-output';
  output.textContent = result.output || '(sem saída)';
  target.append(status, summary, output);
}

function renderError(target, error) {
  target.classList.remove('empty');
  target.innerHTML = `<span class="status FAILED">ERRO</span><pre></pre>`;
  target.querySelector('pre').textContent = error.message;
}

$('runPersonal').addEventListener('click', async () => {
  const target = $('personalResult');
  target.textContent = 'Executando...';
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
  } catch (error) { renderError(target, error); }
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
