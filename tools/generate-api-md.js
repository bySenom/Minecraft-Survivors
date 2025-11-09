#!/usr/bin/env node
// generate-api-md.js
// Simple Java source scraper for basic API docs: extracts package, class, Javadoc comments and public method signatures

const fs = require('fs');
const path = require('path');

const srcRoot = path.join(__dirname, '..', 'src', 'main', 'java');
const outRoot = path.join(__dirname, '..', 'docs', 'api');
const wikiApiIndex = path.join(__dirname, '..', 'docs', 'wiki', 'API.md');

function walkDir(dir) {
  let files = [];
  for (const name of fs.readdirSync(dir)) {
    const fp = path.join(dir, name);
    const st = fs.statSync(fp);
    if (st.isDirectory()) files = files.concat(walkDir(fp));
    else if (st.isFile() && fp.endsWith('.java')) files.push(fp);
  }
  return files;
}

function ensureDir(p) { if (!fs.existsSync(p)) fs.mkdirSync(p, { recursive: true }); }

function extractJava(filePath) {
  const src = fs.readFileSync(filePath, 'utf8');
  const lines = src.split(/\r?\n/);
  const pkgMatch = src.match(/package\s+([\w\.]+)\s*;/);
  const pkg = pkgMatch ? pkgMatch[1] : '';
  // Find class/interface/enum declarations (public or package-private)
  const classRegex = /(\/\*\*[\s\S]*?\*\/\s*)?(public\s+)?(abstract\s+|final\s+)?(class|interface|enum)\s+(\w+)/g;
  let m; const classes = [];
  while ((m = classRegex.exec(src)) !== null) {
    const javadoc = m[1] ? m[1].trim() : '';
    const kind = m[4];
    const name = m[5];
    const idx = m.index + m[0].length;
    // slice until next class declaration or end for scanning members
    const rest = src.slice(idx);
    // extract public methods and fields via regex
    const methodRegex = /(?:\/\*\*[\s\S]*?\*\/\s*)?public\s+(?:static\s+)?(?:[\w<>\[\]]+[\s]+)+([\w]+)\s*\(([\s\S]*?)\)\s*(?:throws[^{;]+)?\s*\{/g;
    const fieldRegex = /(?:\/\*\*[\s\S]*?\*\/\s*)?public\s+(?:static\s+)?(?:final\s+)?([\w<>\[\]]+)\s+([\w]+)\s*(?:=|;)/g;
    const methods = [];
    let mm;
    while ((mm = methodRegex.exec(rest)) !== null) {
      const full = mm[0];
      // find preceding Javadoc if any
      const pre = rest.slice(Math.max(0, mm.index - 400), mm.index);
      const docMatch = pre.match(/(\/\*[\s\S]*?\*\/)\s*$/);
      const doc = docMatch ? docMatch[1].trim() : '';
      const name = mm[1];
      const args = mm[2].replace(/\n/g,' ').trim();
      methods.push({name, args, doc});
    }
    const fields = [];
    let fm;
    while ((fm = fieldRegex.exec(rest)) !== null) {
      const pre = rest.slice(Math.max(0, fm.index - 400), fm.index);
      const docMatch = pre.match(/(\/\*[\s\S]*?\*\/)\s*$/);
      const doc = docMatch ? docMatch[1].trim() : '';
      fields.push({type: fm[1], name: fm[2], doc});
    }
    classes.push({pkg, kind, name, javadoc, methods, fields});
  }
  return classes;
}

function javadocToMd(j) {
  if (!j) return '';
  // strip /** */ and leading *
  let s = j.replace(/^\/\*\*/,'').replace(/\*\/$/,'');
  s = s.split(/\r?\n/).map(l => l.replace(/^\s*\*\s?/, '')).join('\n');
  return s.trim();
}

function writeClassMd(cls) {
  const pkgDir = cls.pkg ? cls.pkg.replace(/\./g, path.sep) : '';
  const outDir = path.join(outRoot, pkgDir);
  ensureDir(outDir);
  const fileName = path.join(outDir, cls.name + '.md');
  const lines = [];
  lines.push('# ' + cls.name);
  lines.push('');
  lines.push('Package: `' + (cls.pkg || '(default)') + '`');
  lines.push('');
  if (cls.javadoc) lines.push(javadocToMd(cls.javadoc));
  lines.push('');
  if (cls.fields && cls.fields.length) {
    lines.push('## Public Fields'); lines.push('');
    for (const f of cls.fields) {
      lines.push('- `' + f.type + ' ' + f.name + '`');
      if (f.doc) lines.push('  ' + javadocToMd(f.doc).split('\n').map(l=>'> '+l).join('\n'));
    }
    lines.push('');
  }
  if (cls.methods && cls.methods.length) {
    lines.push('## Public Methods'); lines.push('');
    for (const m of cls.methods) {
      lines.push('- `' + m.name + '(' + m.args + ')`');
      if (m.doc) lines.push('  ' + javadocToMd(m.doc).split('\n').map(l=>'> '+l).join('\n'));
    }
    lines.push('');
  }
  fs.writeFileSync(fileName, lines.join('\n'), 'utf8');
  return path.relative(outRoot, fileName).replace(/\\/g, '/');
}

function generate() {
  ensureDir(outRoot);
  const files = walkDir(srcRoot);
  const indexEntries = [];
  for (const f of files) {
    const classes = extractJava(f);
    for (const c of classes) {
      const rel = writeClassMd(c);
      indexEntries.push({pkg: c.pkg, name: c.name, path: rel});
    }
  }
  // write wiki index
  const idxLines = ['# API Reference', '', 'This section contains automatically generated API documentation (basic).', '', '## Classes', ''];
  indexEntries.sort((a,b) => (a.pkg||'').localeCompare(b.pkg||'') || a.name.localeCompare(b.name));
  for (const e of indexEntries) {
    idxLines.push('- [' + (e.pkg ? e.pkg + '.' : '') + e.name + '](../api/' + e.path + ')');
  }
  fs.writeFileSync(wikiApiIndex, idxLines.join('\n'), 'utf8');
  return indexEntries.length;
}

try {
  const count = generate();
  console.log('Generated API docs for', count, 'classes');
  process.exit(0);
} catch (e) {
  console.error('Error generating API docs:', e);
  process.exit(2);
}
