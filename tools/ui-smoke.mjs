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
assert.match(js, /themeToggle/, 'theme toggle behavior must exist');

console.log('UI_SMOKE_OK');
