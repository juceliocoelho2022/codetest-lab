(() => {
  const runnerHealth = document.getElementById('runnerHealth');
  const runnerHealthLabel = document.getElementById('runnerHealthLabel');

  if (!runnerHealth || !runnerHealthLabel) return;

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
    runnerHealth.disabled = true;

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
      } else if (health.dockerAvailable) {
        setRunnerHealthState('degraded', 'Runner ausente', health.message);
      } else {
        setRunnerHealthState('down', 'Docker indisponível', health.message);
      }
    } catch (error) {
      setRunnerHealthState(
        'down',
        'Health indisponível',
        `Não foi possível verificar o ambiente Docker: ${error.message}`
      );
    } finally {
      runnerHealth.disabled = false;
    }
  }

  runnerHealth.addEventListener('click', refreshRunnerHealth);
  refreshRunnerHealth();
})();
