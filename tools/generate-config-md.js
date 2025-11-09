#!/usr/bin/env node
// Simple generator: reads src/main/resources/config.yml and writes docs/wiki/Config-Reference.md
const fs = require('fs');
const path = require('path');
const yaml = require('js-yaml');

const inPath = path.join(__dirname, '..', 'src', 'main', 'resources', 'config.yml');
const outPath = path.join(__dirname, '..', 'docs', 'wiki', 'Config-Reference.md');

function header() {
  return `# Konfigurationsreferenz (auto-generated)\n\nDiese Seite wurde automatisch aus \`src/main/resources/config.yml\` generiert.\n\n---\n\n`;
}

function walk(obj, prefix = '') {
  let out = '';
  if (typeof obj !== 'object' || obj === null) return out;
  for (const key of Object.keys(obj)) {
    const val = obj[key];
    const full = prefix ? `${prefix}.${key}` : key;
    if (typeof val === 'object' && val !== null && !Array.isArray(val)) {
      out += `## ${full}\n\n`;
      // show keys in table
      out += '| Key | Value (example) | Type |\n|---|---|---|\n';
      for (const k of Object.keys(val)) {
        const v = val[k];
        out += '| ' + k + ' | `' + JSON.stringify(v).replace(/\n/g,' ') + '` | ' + (Array.isArray(v)?'array':typeof v) + ' |\n';
      }
      out += '\n';
      out += walk(val, full);
    }
  }
  return out;
}

try {
  const content = fs.readFileSync(inPath, 'utf8');
  const parsed = yaml.load(content);
  let md = header();
  // Top-level sections list
  md += '## Top-level keys\n\n';
  for (const k of Object.keys(parsed)) {
    md += '- `' + k + '`\n';
  }
  md += '\n---\n\n';
  md += walk(parsed);
  fs.writeFileSync(outPath, md, 'utf8');
  console.log('Wrote', outPath);
  process.exit(0);
} catch (e) {
  console.error('Error generating config doc:', e);
  process.exit(2);
}
