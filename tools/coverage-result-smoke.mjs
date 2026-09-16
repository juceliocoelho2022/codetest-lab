import fs from 'node:fs';
import assert from 'node:assert/strict';

const coveragePath = 'src/main/resources/static/result-coverage.css';
const studentCss = fs.readFileSync('src/main/resources/static/student-editor.css', 'utf8');

assert.match(
  studentCss,
  /@import\s+url\(['"]\/result-coverage\.css['"]\)/,
  'coverage result stylesheet must be loaded'
);
assert.ok(fs.existsSync(coveragePath), 'coverage result stylesheet must exist');

const css = fs.readFileSync(coveragePath, 'utf8');
assert.match(
  css,
  /#personalResult:has\(\.coverage-section\)/,
  'coverage result must auto-expand when JaCoCo is rendered'
);
assert.match(css, /max-height:\s*220px/, 'desktop coverage result must get more vertical room');
assert.match(
  css,
  /@media \(max-height:\s*700px\) and \(min-width:\s*821px\)/,
  'short desktop must have a dedicated coverage layout'
);
assert.match(
  css,
  /#personalResult:has\(\.coverage-section\)[\s\S]*max-height:\s*150px/,
  'short desktop coverage must remain visible without taking the whole viewport'
);
assert.match(
  css,
  /\.coverage-note\s*\{[^}]*display:\s*none/,
  'short viewport must hide the explanatory note to prioritize metrics'
);

console.log('COVERAGE_RESULT_SMOKE_OK');
