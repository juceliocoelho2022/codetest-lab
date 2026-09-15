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

// v0.3.1 professional execution-profile cards.
assert.match(html, /id="executionProfile"[^>]*type="hidden"|type="hidden"[^>]*id="executionProfile"/, 'execution profile state must remain available to the API');
for (const profile of [
  'JUNIT5',
  'JUNIT5_MOCKITO',
  'JUNIT5_JACOCO',
  'JUNIT5_MOCKITO_JACOCO'
]) {
  assert.ok(html.includes(`data-profile="${profile}"`), `profile card ${profile} must exist`);
}
assert.match(html, /class="profile-card[^\"]*active[^\"]*"[^>]*data-profile="JUNIT5_MOCKITO"|data-profile="JUNIT5_MOCKITO"[^>]*class="profile-card[^\"]*active/, 'JUnit 5 + Mockito card must be selected by default');
assert.match(html, /id="profileHelpTitle"/, 'active profile title must exist');
assert.match(html, /id="profileHelp"/, 'active profile description must exist');
assert.match(js, /function selectExecutionProfile\(/, 'profile-card selection behavior must exist');
assert.match(js, /executionProfile:\s*\$\(['"]executionProfile['"]\)\.value/, 'selected profile must be sent to the API');
assert.match(css, /\.profile-card\.active/, 'active profile card styling must exist');

// v0.3.1 numbered code editors and diagnostic navigation.
assert.match(html, /id="personalSourceLines"/, 'Java editor line gutter must exist');
assert.match(html, /id="personalTestLines"/, 'JUnit editor line gutter must exist');
assert.match(html, /class="code-editor/, 'numbered editor shell must exist');
assert.match(js, /function setupNumberedEditor\(/, 'numbered editor setup must exist');
assert.match(js, /function renderLineNumbers\(/, 'line-number rendering must exist');
assert.match(js, /function jumpToEditorLine\(/, 'diagnostic line navigation must exist');
assert.match(js, /Ir para linha/, 'result must offer jump-to-line action');
assert.match(css, /\.line-gutter/, 'line gutter styling must exist');
assert.match(css, /\.line-number\.error-line/, 'error line styling must exist');

// Regression: profile cards may not squeeze the editors on short/wide viewports.
assert.match(css, /\.class-run-row\s*\{[\s\S]*?grid-template-areas:\s*["']class actions["'][\s\S]*?["']profile profile["']/, 'desktop controls must reserve a full-width row for profiles');
assert.match(css, /\.profile-field\s*\{[\s\S]*?grid-area:\s*profile/, 'profile selector must occupy the full controls row');
assert.match(css, /\.class-field\s*\{[^}]*grid-area:\s*class/, 'class field must use the compact controls row');
assert.match(css, /\.run-actions\s*\{[^}]*grid-area:\s*actions/, 'run actions must share the compact controls row');
assert.match(css, /@media \(max-height:\s*700px\) and \(min-width:\s*821px\)[\s\S]*?\.profile-help-box\s*\{\s*display:\s*none/, 'short desktop viewport must collapse profile help copy');
assert.match(css, /@media \(max-height:\s*700px\) and \(min-width:\s*821px\)[\s\S]*?\.personal-panel\.active[\s\S]*?grid-template-rows:\s*auto auto minmax\(120px,\s*1fr\) auto/, 'short desktop viewport must preserve usable editor height');
assert.match(css, /#personalResult\s*\{[^}]*max-height:\s*210px/, 'desktop result must use bounded internal space instead of squeezing editors');

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

console.log('UI_SMOKE_V232_COMPACT_OK');
