import fs from 'node:fs';
import assert from 'node:assert/strict';

const html = fs.readFileSync('src/main/resources/static/index.html', 'utf8');
const css = fs.readFileSync('src/main/resources/static/styles.css', 'utf8');
const js = fs.readFileSync('src/main/resources/static/app.js', 'utf8');

assert.match(html, /id="themeToggle"/, 'theme toggle button must exist');
assert.match(html, /data-theme/, 'theme must be initialized in the document');
assert.match(css, /html\[data-theme="light"\]/, 'light theme variables must exist');
assert.match(css, /@media \(min-width: 821px\)[\s\S]*body[\s\S]*overflow:\s*hidden/, 'desktop page scrolling must be disabled');
assert.match(css, /\.result[\s\S]*overflow:\s*auto/, 'result log must scroll internally');
assert.match(js, /const THEME_KEY = ['"]codetest-theme['"]/, 'theme key must be defined');
assert.match(js, /localStorage\.setItem\(THEME_KEY/, 'theme preference must be persisted');

assert.match(html, /id="clearPersonalResult"/, 'clear result action must exist');
assert.match(html, /<kbd>Ctrl<\/kbd>\s*\+\s*<kbd>Enter<\/kbd>/, 'keyboard shortcut hint must be visible');
assert.match(js, /function parseExecutionDetails\(/, 'friendly execution parser must exist');
assert.ok(js.includes('expected:\\s*<(.+?)>\\s*but was:\\s*<(.+?)>'), 'JUnit expected/actual parsing must exist');
assert.match(js, /COMPILE_ERROR/, 'compile error must have friendly handling');
assert.match(js, /Todos os testes foram concluídos com sucesso/, 'passed result must have friendly message');
assert.match(js, /document\.addEventListener\(['"]keydown['"]/, 'keyboard shortcut listener must exist');
assert.match(js, /\(event\.ctrlKey \|\| event\.metaKey\)[\s\S]*event\.key === ['"]Enter['"]/, 'Ctrl/Cmd+Enter must execute tests');
assert.match(js, /clearPersonalResult/, 'clear result behavior must exist');
assert.match(js, /document\.createElement\(['"]details['"]\)/, 'technical log must use collapsible details');
assert.match(css, /\.result-message/, 'friendly result message styling must exist');
assert.match(css, /\.result-details/, 'collapsible technical details styling must exist');

console.log('UI_SMOKE_V21_OK');
