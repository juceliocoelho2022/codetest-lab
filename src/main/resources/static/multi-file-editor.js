(() => {
  const MAX_SOURCE_FILES = 10;
  const MAX_SOURCE_CHARS = 100_000;
  const JAVA_FILE_NAME = /^[A-Za-z_$][A-Za-z\d_$]*\.java$/;

  const sourceTextarea = document.getElementById('personalSource');
  const classNameInput = document.getElementById('personalClassName');
  const tabs = document.getElementById('sourceFileTabs');
  const addButton = document.getElementById('addSourceFile');
  const renameButton = document.getElementById('renameSourceFile');
  const deleteButton = document.getElementById('deleteSourceFile');

  if (!sourceTextarea || !classNameInput || !tabs || !addButton || !renameButton || !deleteButton) {
    return;
  }

  let files = [{
    fileName: `${classNameInput.value.trim()}.java`,
    content: sourceTextarea.value
  }];
  let activeIndex = 0;

  function primaryFileName() {
    const className = classNameInput.value.trim();
    return className ? `${className}.java` : '';
  }

  function persistActiveContent() {
    if (!files[activeIndex]) return;
    files[activeIndex].content = sourceTextarea.value;
  }

  function loadActiveContent() {
    const active = files[activeIndex];
    if (!active) return;
    sourceTextarea.value = active.content;
    sourceTextarea.scrollTop = 0;
    sourceTextarea.dispatchEvent(new Event('input', {bubbles: true}));
  }

  function renderTabs() {
    tabs.replaceChildren();

    files.forEach((file, index) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = `source-file-tab${index === activeIndex ? ' active' : ''}`;
      button.dataset.fileName = file.fileName;
      button.setAttribute('role', 'tab');
      button.setAttribute('aria-selected', String(index === activeIndex));
      button.title = file.fileName;
      button.textContent = file.fileName;
      button.addEventListener('click', () => switchTo(index));
      tabs.appendChild(button);
    });

    const active = files[activeIndex];
    const isPrimary = active?.fileName === primaryFileName();
    deleteButton.disabled = files.length === 1 || isPrimary;
    deleteButton.title = isPrimary
      ? 'O arquivo da classe principal não pode ser excluído.'
      : files.length === 1
        ? 'Mantenha ao menos um arquivo Java.'
        : 'Excluir arquivo ativo';
    addButton.disabled = files.length >= MAX_SOURCE_FILES;
  }

  function switchTo(index) {
    if (index < 0 || index >= files.length || index === activeIndex) return;
    persistActiveContent();
    activeIndex = index;
    loadActiveContent();
    renderTabs();
  }

  function nextFileName() {
    const used = new Set(files.map(file => file.fileName.toLowerCase()));
    let suffix = 1;
    while (true) {
      const className = suffix === 1 ? 'NovaClasse' : `NovaClasse${suffix}`;
      const candidate = `${className}.java`;
      if (!used.has(candidate.toLowerCase())) return candidate;
      suffix++;
    }
  }

  function createFile() {
    persistActiveContent();
    if (files.length >= MAX_SOURCE_FILES) {
      window.alert('O limite é de 10 arquivos Java.');
      return;
    }

    const fileName = nextFileName();
    const className = fileName.slice(0, -'.java'.length);
    files.push({
      fileName,
      content: `public class ${className} {\n}\n`
    });
    activeIndex = files.length - 1;
    loadActiveContent();
    renderTabs();
  }

  function validateRename(newName) {
    const candidate = String(newName || '').trim();
    if (!JAVA_FILE_NAME.test(candidate)) {
      throw new Error('Use um nome Java válido terminado em .java.');
    }

    const duplicate = files.some((file, index) =>
      index !== activeIndex && file.fileName.toLowerCase() === candidate.toLowerCase());
    if (duplicate) {
      throw new Error('Já existe um arquivo Java com esse nome.');
    }
    return candidate;
  }

  function renameActiveFile(newName) {
    persistActiveContent();
    const active = files[activeIndex];
    if (!active) return;

    const candidate = validateRename(newName);
    const wasPrimary = active.fileName === primaryFileName();
    active.fileName = candidate;

    if (wasPrimary) {
      classNameInput.value = candidate.slice(0, -'.java'.length);
    }
    renderTabs();
  }

  function deleteActiveFile() {
    persistActiveContent();
    const active = files[activeIndex];
    if (!active) return;

    if (files.length === 1) {
      window.alert('Mantenha ao menos um arquivo Java.');
      return;
    }
    if (active.fileName === primaryFileName()) {
      window.alert('O arquivo da classe principal não pode ser excluído.');
      return;
    }
    if (!window.confirm(`Excluir ${active.fileName}?`)) return;

    files.splice(activeIndex, 1);
    activeIndex = Math.min(activeIndex, files.length - 1);
    loadActiveContent();
    renderTabs();
  }

  function activateFile(fileName) {
    const index = files.findIndex(file => file.fileName === fileName);
    if (index < 0) return false;
    if (index !== activeIndex) switchTo(index);
    return true;
  }

  function getActiveFileName() {
    return files[activeIndex]?.fileName || '';
  }

  function getFiles() {
    persistActiveContent();

    const total = files.reduce((sum, file) => sum + file.content.length, 0);
    if (total > MAX_SOURCE_CHARS) {
      throw new Error('O código-fonte excede o limite total de 100.000 caracteres.');
    }

    const primary = primaryFileName();
    if (!primary || !files.some(file => file.fileName === primary)) {
      throw new Error('O arquivo da classe principal não existe.');
    }

    return files.map(file => ({...file}));
  }

  addButton.addEventListener('click', createFile);
  renameButton.addEventListener('click', () => {
    const current = getActiveFileName();
    const next = window.prompt('Novo nome do arquivo Java:', current);
    if (next === null || next.trim() === current) return;
    try {
      renameActiveFile(next);
    } catch (error) {
      window.alert(error.message);
    }
  });
  deleteButton.addEventListener('click', deleteActiveFile);
  classNameInput.addEventListener('input', renderTabs);

  window.codeTestSourceWorkspace = {
    getFiles: getFiles,
    activateFile: activateFile,
    getActiveFileName: getActiveFileName,
    renameActiveFile: renameActiveFile,
    createFile: createFile,
    deleteActiveFile: deleteActiveFile
  };

  renderTabs();
})();
