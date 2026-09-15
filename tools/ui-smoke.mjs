import fs from 'node:fs';
import assert from 'node:assert/strict';

const html = fs.readFileSync('src/main/resources/static/index.html', 'utf8');
const css = fs.readFileSync('src/main/resources/static/styles.css', 'utf8');
const js = fs.readFileSync('src/main/resources/static/app.js', 'utf8');

// Theme and viewport behavior from v0.2.x.
assert.match(html, /id="themeToggle"/, 'theme toggle button must exist');
assert.match(html, /data-theme/, 'theme must be initialized in the document');
assert.match(css, /html\[data-theme="light"\]/, 'light theme variables must exist');
assert.match(css, /@media \(min-width: 821px\)[\s\S]*body[\s\S]*overflow:\s*hidden/, 'desktop page scrolling must be disabled');
assert.match(js, /const THEME_KEY = ['"]codetest-theme['"]/, 'theme key must be defined');
assert.match(js, /localStorage\.setItem\(THEME_KEY/, 'theme preference must be persisted');

// Existing personal playground conveniences.
assert.match(html, /id="clearPersonalResult"/, 'clear result action must exist');
assert.match(html, /<kbd>Ctrl<\/kbd>\s*\+\s*<kbd>Enter<\/kbd>/, 'keyboard shortcut hint must be visible');
assert.match(js, /document\.addEventListener\(['"]keydown['"]/, 'keyboard shortcut listener must exist');
assert.match(js, /clearPersonalResult/, 'clear result behavior must exist');

// v0.3 execution profiles.
assert.match(html, /id="executionProfile"/, 'execution profile selector must exist');
for (const profile of [
  'JUNIT5',
  'JUNIT5_MOCKITO',
  'JUNIT5_JACOCO',
  'JUNIT5_MOCKITO_JACOCO'
]) {
  assert.ok(html.includes(`value="${profile}"`), `profile ${profile} must exist`);
}
assert.match(html, /value="JUNIT5_MOCKITO"\s+selected/, 'JUnit 5 + Mockito must be the default UI profile');
assert.match(js, /executionProfile:\s*\$\(['"]executionProfile['"]\)\.value/, 'selected profile must be sent to the API');

// v2.2 result dashboard.
assert.match(js, /function formatDuration\(/, 'human duration formatter must exist');
assert.match(js, /function resultPresentation\(/, 'status-specific presentation must exist');
assert.match(js, /function appendCoverage\(/, 'conditional coverage renderer must exist');
assert.match(js, /result\.coverage/, 'coverage must be read from the API result');
assert.match(js, /result\.status\s*!==\s*['"]COMPILE_ERROR['"][\s\S]*result\.testsRun\s*>\s*0/, 'compile errors must not render meaningless test counters');
assert.match(js, /Todos os testes foram concluídos com sucesso/, 'passed result must have friendly message');
assert.match(js, /Verifique a instrução próxima à linha/, 'compile error must have guidance');
assert.match(js, /document\.createElement\(['"]details['"]\)/, 'technical log must use collapsible details');
assert.match(css, /\.result-card/, 'result dashboard card styling must exist');
assert.match(css, /\.result-status-title/, 'result title styling must exist');
assert.match(css, /\.coverage-grid/, 'coverage grid styling must exist');
assert.match(css, /\.coverage-bar/, 'coverage bars must exist');
assert.match(css, /\.result\.COMPILE_ERROR|\.result-card\.compile-error/, 'compile-error visual state must exist');

// Failure parsing remains available.
assert.match(js, /function parseExecutionDetails\(/, 'friendly execution parser must exist');
assert.ok(js.includes('expected:\\s*<(.+?)>\\s*but was:\\s*<(.+?)>'), 'JUnit expected/actual parsing must exist');

console.log('UI_SMOKE_V22_PROFILES_OK');
