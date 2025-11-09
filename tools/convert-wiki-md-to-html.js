#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const md = require('markdown-it')({html:true}).use(require('markdown-it-anchor'));

const wikiDir = path.join(__dirname, '..', 'docs', 'wiki');
if (!fs.existsSync(wikiDir)) { console.error('wiki dir not found:', wikiDir); process.exit(1); }

const files = fs.readdirSync(wikiDir).filter(f => f.endsWith('.md'));
console.log('Converting', files.length, 'files...');

const template = (title, content) => `<!doctype html>
<html lang="de">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>${title} — Minecraft Survivors</title>
<style>body{font-family:Inter,system-ui,Arial,sans-serif;margin:24px;max-width:980px;color:#112;line-height:1.5} pre{background:#f6f8fa;padding:12px;border-radius:6px;overflow:auto} code{background:#eef;padding:2px 4px;border-radius:4px}</style>
</head>
<body>
<article>
<h1>${title}</h1>
${content}
</article>
<p><a href="/">Zur Startseite</a> • <a href="/docs/wiki/Home.html">Wiki</a></p>
</body>
</html>`;

for (const f of files) {
  try {
    const inPath = path.join(wikiDir, f);
    const txt = fs.readFileSync(inPath, 'utf8');
    const html = md.render(txt);
    // Der Title: first heading or filename
    const match = txt.match(/^#\s+(.+)$/m);
    const title = match ? match[1].trim() : f.replace(/\.md$/,'');
    const outPath = path.join(wikiDir, f.replace(/\.md$/, '.html'));
    fs.writeFileSync(outPath, template(title, html), 'utf8');
    console.log('Wrote', outPath);
  } catch (e) {
    console.error('Error converting', f, e.message);
  }
}
console.log('Done');

