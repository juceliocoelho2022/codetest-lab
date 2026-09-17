import fs from 'node:fs';
import assert from 'node:assert/strict';

const html = fs.readFileSync('src/main/resources/static/index.html', 'utf8');
const css = fs.readFileSync('src/main/resources/static/styles.css', 'utf8');
const studentCss = fs.existsSync('src/main/resources/static/student-editor.css')
  ? fs.readFileSync('src/main/resources/static/student-editor.css', 'utf8')
  : '';
const healthCss = fs.existsSync('src/main/resources/static/runner-health.css')
  ? fs.readFileSync('src/main/resources/static/runner-health.css', 'utf8')
  : '';
const multiFileCss = fs.existsSync('src/main/resources/static/multi-file-editor.css')
  ? fs.readFileSync('src/main/resources/static/multi-file-editor.css', 'utf8')
  : '';
const js = fs.readFileSync('src/main/resources/static/app.js', 'utf8');
const healthJs = fs.existsSync('src/main/resources/static/runner-health.js')
  ? fs.readFileSync('src/main/resources/static/runner-health.js', 'utf8')
  : '';
const multiFileJs = fs.existsSync('src/main/resources/static/multi-file-editor.js')
  ? fs.readFileSync('src/main/resources/static/multi-file-editor.js', 'utf8')
  : '';
const frontendCode = `${html}\n${js}`;

// Theme and viewport behavior.
assert.match(html, /id="themeToggle"/, 'theme toggle button must exist');
assert.match(html, /data-theme/, 'theme must be initialized in the document');
assert.match(css, /html\[data-theme="light"\]/, 'light theme variables must exist');
assert.match(css, /@media \(min-width: 821px\)[\s\S]*body[\s\S]*overflow:\s*hidden/, 'desktop page scrolling must be disabled');
assert.match(js, /const THEME_KEY = ['"]codetest-theme['"]/, 'theme key must be defined');

// Playground actions.
assert.match(html, /id="clearPersonalResult"/, 'clear result action must exist');
assert.match(html, /<kbd>Ctrl<\/kbd>\s*\+\s*<kbd>Enter<\/kbd>/, 'keyboard shortcut hint must be visible');
assert.match(js, /document\.addEventListener\(['"]keydown['"]/, 'keyboard shortcut listener must exist');

// v0.3.2 compact execution-profile chooser.
assert.match(html, /<details[^>]*id="profileDropdown"/, 'execution profiles must live in a dropdown');
assert.match(html, /<summary[^>]*id="profileDropdownToggle"/, 'dropdown trigger must exist');
assert.match(html, /id="executionProfile"[^>]*type="hidden"|type="hidden"[^>]*id="executionProfile"/, 'selected execution profile must remain available to the API');
assert.match(html, /class="profile-popover"/, 'profile options must render in a floating popover');
for (const profile of [
  'JUNIT5',
  'JUNIT5_MOCKITO',
  'JUNIT5_JACOCO',
  'JUNIT5_MOCKITO_JACOCO'
]) {
  assert.ok(html.includes(`data-profile="${profile}"`), `profile ${profile} must remain selectable`);
}
assert.match(css, /\.profile-dropdown\s*\{[\s\S]*?position:\s*relative/, 'profile dropdown must anchor its popover');
assert.match(css, /\.profile-popover\s*\{[\s\S]*?position:\s*absolute/, 'profile popover must float without consuming editor height');
assert.match(css, /\.profile-dropdown\[open\][\s\S]*\.profile-popover/, 'open dropdown styling must exist');
assert.match(js, /executionProfile:\s*\$\(['"]executionProfile['"]\)\.value/, 'selected profile must be sent to the API');

// Numbered code editors and diagnostic navigation.
assert.match(html, /id="personalSourceLines"/, 'Java editor line gutter must exist');
assert.match(html, /id="personalTestLines"/, 'JUnit editor line gutter must exist');
assert.match(html, /class="code-editor/, 'numbered editor shell must exist');
assert.match(js, /function setupNumberedEditor\(/, 'numbered editor setup must exist');
assert.match(js, /function jumpToEditorLine\(/, 'diagnostic line navigation must exist');
assert.match(js, /Ir para linha/, 'result must offer jump-to-line action');
assert.match(css, /\.line-gutter/, 'line gutter styling must exist');
assert.match(css, /\.line-number\.error-line/, 'error line styling must exist');

// Student submission editor: large, full-width and numbered.
assert.match(html, /student-editor\.css/, 'student editor stylesheet must be loaded');
assert.match(html, /id="studentSourceLines"/, 'student Java editor line gutter must exist');
assert.match(html, /class="student-code-area"/, 'student solution must use a dedicated full-width editor area');
assert.match(html, /class="student-code-editor/, 'student solution must use the numbered editor shell');
assert.match(frontendCode, /setupNumberedEditor\(['"]studentSource['"],\s*['"]studentSourceLines['"]\)/, 'student editor must initialize line numbers');
assert.match(studentCss, /\.student-code-area\s*\{[\s\S]*?grid-column:\s*1\s*\/\s*-1/, 'student editor must span the full submission width');
assert.match(studentCss, /\.student-code-editor\s*\{[\s\S]*?min-height:\s*340px/, 'student editor must be substantially taller on desktop');
assert.match(studentCss, /\.student-upload-row\s*\{[\s\S]*?display:\s*grid/, 'ZIP upload must become a secondary row');

// Regression: controls must stay compact and editors must dominate the viewport.
assert.match(css, /\.class-run-row\s*\{[\s\S]*?grid-template-columns:\s*minmax\(180px,[^;]+\)\s+minmax\(240px,[^;]+\)\s+auto/, 'desktop controls must fit class, profile chooser and actions in one row');
assert.match(css, /\.personal-panel\.active\s*\{[\s\S]*?grid-template-rows:\s*auto auto minmax\(180px,\s*1fr\) auto/, 'desktop layout must reserve at least 180px for editors');
assert.match(css, /@media \(max-height:\s*700px\) and \(min-width:\s*821px\)[\s\S]*?\.code-editor\s*\{[^}]*min-height:\s*170px/, 'short desktop viewport must still preserve a large editor');
assert.match(css, /#personalResult\s*\{[^}]*max-height:\s*150px/, 'result panel must be bounded so editors keep priority');

// Result dashboard.
assert.match(js, /function formatDuration\(/, 'human duration formatter must exist');
assert.match(js, /function resultPresentation\(/, 'status-specific presentation must exist');
assert.match(js, /function appendCoverage\(/, 'conditional coverage renderer must exist');
assert.match(js, /result\.coverage/, 'coverage must be read from the API result');
assert.match(js, /result\.status\s*!==\s*['"]COMPILE_ERROR['"][\s\S]*result\.testsRun\s*>\s*0/, 'compile errors must not render meaningless test counters');
assert.match(js, /document\.createElement\(['"]details['"]\)/, 'technical log must use collapsible details');
assert.match(css, /\.result-card/, 'result dashboard card styling must exist');
assert.match(css, /\.coverage-grid/, 'coverage grid styling must exist');

// v0.3.4 Docker/runner health indicator.
assert.match(html, /runner-health\.css/, 'runner health stylesheet must be loaded');
assert.match(html, /runner-health\.js/, 'runner health script must be loaded');
assert.match(html, /id="runnerHealth"/, 'runner health button must exist');
assert.match(html, /id="runnerHealthLabel"/, 'runner health label must exist');
assert.match(healthJs, /\/api\/v1\/health\/runner/, 'runner health UI must call the health endpoint');
assert.match(healthJs, /refreshRunnerHealth/, 'runner health must support refresh');
assert.match(healthJs, /runnerHealth\.addEventListener\(['"]click['"]/, 'health indicator must be manually refreshable');
assert.match(healthCss, /\.runner-health\.up/, 'online health state must be styled');
assert.match(healthCss, /\.runner-health\.degraded/, 'degraded health state must be styled');
assert.match(healthCss, /\.runner-health\.down/, 'down health state must be styled');

// v0.3.5 Execution Guard.
assert.match(healthJs, /executionGuard/, 'execution guard UI must be created by the health module');
assert.match(healthJs, /executionGuardMessage/, 'execution guard explanation must be maintained');
assert.match(healthJs, /function setExecutionAvailability\(/, 'health UI must control execution availability');
assert.match(healthJs, /runPersonal/, 'personal execution button must be guarded');
assert.match(healthJs, /submitSource/, 'student source submission must be guarded');
assert.match(healthJs, /submitZip/, 'student ZIP submission must be guarded');
assert.match(healthJs, /disabled\s*=\s*!ready/, 'execution controls must be disabled while environment is not ready');
assert.match(healthCss, /\.execution-guard/, 'execution guard message must be styled');
assert.match(healthCss, /\.execution-guard\.visible/, 'execution guard visible state must be styled');

// v0.4.0 multi-file personal workspace.
assert.match(html, /id="sourceFileTabs"/, 'source file tab bar must exist');
assert.match(html, /id="addSourceFile"/, 'new source file action must exist');
assert.match(html, /id="renameSourceFile"/, 'rename source file action must exist');
assert.match(html, /id="deleteSourceFile"/, 'delete source file action must exist');
assert.match(html, /multi-file-editor\.css/, 'multi-file editor stylesheet must load');
assert.match(html, /multi-file-editor\.js/, 'multi-file editor script must load');
assert.match(html, /Classe principal/, 'personal class field must identify the primary class');
assert.match(html, /MVP 0\.4\.0/, 'MVP badge must identify v0.4.0');
assert.doesNotMatch(healthJs, /MVP 0\.3\.5/, 'health module must not overwrite the v0.4.0 badge');
assert.match(multiFileJs, /MAX_SOURCE_FILES\s*=\s*10/, 'UI must enforce ten source files');
assert.match(multiFileJs, /MAX_SOURCE_CHARS\s*=\s*100_000/, 'browser workspace must enforce aggregate source size');
assert.match(multiFileJs, /getFiles\s*:/, 'workspace API must expose getFiles');
assert.match(multiFileJs, /activateFile\s*:/, 'workspace API must expose activateFile');
assert.match(multiFileJs, /function activateFile\(/, 'workspace must support file activation by filename');
assert.match(multiFileJs, /renameActiveFile/, 'source files must be renameable');
assert.match(multiFileJs, /deleteActiveFile/, 'source files must be deletable');
assert.match(multiFileCss, /\.source-file-tabs/, 'source file tabs must be styled');
assert.match(js, /sourceFiles:\s*window\.codeTestSourceWorkspace\.getFiles\(\)/,
  'personal execution must send sourceFiles');
assert.doesNotMatch(js, /sourceCode:\s*\$\(['"]personalSource['"]\)\.value/,
  'personal browser flow must stop sending legacy sourceCode');
assert.match(js, /codeTestSourceWorkspace\.activateFile\(details\.file\)/,
  'compile diagnostics must activate the reported Java source file');

console.log('UI_SMOKE_V040_MULTI_FILE_OK');
