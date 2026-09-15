import fs from 'node:fs';
import assert from 'node:assert/strict';

const html = fs.readFileSync('src/main/resources/static/index.html', 'utf8');
const css = fs.readFileSync('src/main/resources/static/styles.css', 'utf8');
const js = fs.readFileSync('src/main/resources/static/app.js', 'utf8');

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

console.log('UI_SMOKE_V032_DROPDOWN_EDITORS_OK');
