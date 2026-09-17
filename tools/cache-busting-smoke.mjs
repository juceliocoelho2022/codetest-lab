import fs from 'node:fs';
import assert from 'node:assert/strict';

const html = fs.readFileSync('src/main/resources/static/index.html', 'utf8');
const studentCss = fs.readFileSync('src/main/resources/static/student-editor.css', 'utf8');

for (const asset of [
  'styles.css',
  'student-editor.css',
  'runner-health.css',
  'multi-file-editor.css',
  'app.js',
  'multi-file-editor.js',
  'runner-health.js'
]) {
  const escaped = asset.replace('.', '\\.');
  assert.match(
    html,
    new RegExp(`/${escaped}\\?v=0\\.4\\.0`),
    `${asset} must use the v0.4.0 cache-busting query string`
  );
}

assert.match(
  studentCss,
  /result-coverage\.css\?v=0\.4\.0/,
  'indirect JaCoCo coverage stylesheet must also be cache-busted'
);

console.log('CACHE_BUSTING_V040_OK');
