(() => {
  const runnerHealth = document.getElementById('runnerHealth');
  const runnerHealthLabel = document.getElementById('runnerHealthLabel');
  const executionButtons = [
    document.getElementById('runPersonal'),
    document.getElementById('submitSource'),
    document.getElementById('submitZip')
  ].filter(Boolean);

  const badge = document.querySelector('.badge');
  if (badge) badge.textContent = 'MVP 0.3.5';

  if (!runnerHealth || !runnerHealthLabel) return;

  const shell = document.querySelector('.shell');
  const tabs = shell?.querySelector('.tabs');
  const executionGuard = document.createElement('div');
  executionGuard.id = 'executionGuard';
  executionGuard.className = 'execution-guard checking visible';
  executionGuard.setAttribute('role', 'status');
  executionGuard.setAttribute('aria-live', 'polite');
  executionGuard.innerHTML = `
    <span class="execution-guard-icon" aria-hidden="true">!</span>
    <span class="execution-guard-copy">
      <strong>Ambiente de execução</strong>
      <span id="executionGuardMessage">Verificando Docker e a imagem runner antes de liberar as execuções.</span>
    </span>
    <button id="executionGuardRetry" class="execution-guard-retry" type="button">↻ Verificar novamente</button>`;

  if (tabs) tabs.insertAdjacentElement('afterend', executionGuard);

  const executionGuardMessage = document.getElementById('executionGuardMessage');
  const executionGuardRetry = document.getElementById('executionGuardRetry');
  let environmentReady = false;

  function setExecutionAvailability(ready, state = 'down', message = '') {
    environmentReady = Boolean(ready);

    for (const button of executionButtons) {
      if (!environmentReady) {
        button.disabled = true;
        button.dataset.executionGuard = 'blocked';
        button.setAttribute('aria-disabled', 'true');
      } else {
        button.dataset.executionGuard = 'ready';
        button.setAttribute('aria-disabled', 'false');
        const executionInProgress = button.id === 'runPersonal'
          && button.textContent.includes('Executando');
        if (!executionInProgress) button.disabled = false;
      }
    }

    if (environmentReady) {
      executionGuard.className = 'execution-guard';
      executionGuard.hidden = true;
      return;
    }

    executionGuard.hidden = false;
    executionGuard.className = `execution-guard ${state} visible`;
    if (executionGuardMessage) executionGuardMessage.textContent = message;
  }

  function setRunnerHealthState(state, label, message) {
    const normalized = ['up', 'degraded', 'down', 'checking'].includes(state)
      ? state
      : 'down';

    runnerHealth.className = `runner-health ${normalized}`;
    runnerHealthLabel.textContent = label;
    runnerHealth.title = message || label;
    runnerHealth.setAttribute('aria-label', `${label}. Clique para verificar novamente.`);
    runnerHealth.dataset.status = normalized.toUpperCase();
  }

  async function refreshRunnerHealth() {
    setRunnerHealthState('checking', 'Verificando Docker…', 'Verificando Docker e imagem runner.');
    setExecutionAvailability(
      false,
      'checking',
      'Verificando Docker e a imagem runner antes de liberar as execuções.'
    );
    runnerHealth.disabled = true;
    if (executionGuardRetry) executionGuardRetry.disabled = true;

    try {
      const response = await fetch('/api/v1/health/runner', {
        method: 'GET',
        headers: {'Accept': 'application/json'},
        cache: 'no-store'
      });

      if (!response.ok) {
        throw new Error(`Health HTTP ${response.status}`);
      }

      const health = await response.json();
      if (health.status === 'UP' && health.dockerAvailable && health.runnerImageAvailable) {
        setRunnerHealthState('up', 'Docker Online', health.message);
        setExecutionAvailability(true, 'up', '');
      } else if (health.dockerAvailable) {
        setRunnerHealthState('degraded', 'Runner ausente', health.message);
        setExecutionAvailability(
          false,
          'degraded',
          `Docker está online, mas a imagem ${health.image || 'codetest-lab-runner:latest'} não está disponível. Recrie o runner e verifique novamente.`
        );
      } else {
        setRunnerHealthState('down', 'Docker indisponível', health.message);
        setExecutionAvailability(
          false,
          'down',
          'Docker indisponível. Inicie o Docker Desktop e clique em “Verificar novamente”.'
        );
      }
    } catch (error) {
      setRunnerHealthState(
        'down',
        'Health indisponível',
        `Não foi possível verificar o ambiente Docker: ${error.message}`
      );
      setExecutionAvailability(
        false,
        'down',
        'Não foi possível verificar o ambiente de execução. Confirme se a aplicação e o Docker Desktop estão ativos.'
      );
    } finally {
      runnerHealth.disabled = false;
      if (executionGuardRetry) executionGuardRetry.disabled = false;
    }
  }

  // Defense in depth: if another script removes disabled while health is DOWN,
  // immediately restore the execution guard state.
  for (const button of executionButtons) {
    new MutationObserver(() => {
      if (!environmentReady && !button.disabled) button.disabled = true;
    }).observe(button, {attributes: true, attributeFilter: ['disabled']});
  }

  document.addEventListener('click', (event) => {
    const executionButton = event.target.closest?.('#runPersonal, #submitSource, #submitZip');
    if (!executionButton || environmentReady) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    executionGuard.classList.add('attention');
    window.setTimeout(() => executionGuard.classList.remove('attention'), 450);
  }, true);

  runnerHealth.addEventListener('click', refreshRunnerHealth);
  executionGuardRetry?.addEventListener('click', refreshRunnerHealth);

  window.codeTestExecutionGuard = {
    isReady: () => environmentReady,
    refresh: refreshRunnerHealth
  };

  refreshRunnerHealth();
})();
