Windows: Git-Befehle (cmd.exe und PowerShell)

Kurzreferenz — sichere, copy/paste-fähige Beispiele für Windows-Umgebungen.

1) In einer reinen cmd.exe (cmd prompt)

- Stage + Commit + Push (alles in einer Zeile):

```cmd
git add -A && git commit -m "Mein Commit-Text" && git push
```

2) Aus PowerShell heraus: starte cmd /c mit einfachen Anführungszeichen um die gesamte cmd-Kette

- Empfohlen (einfach und robust):

```powershell
cmd /c 'git add -A && git commit -m "Mein Commit-Text" && git push'
```

- Alternative (doppelte Anführungszeichen, inneres Zitat escapen):

```powershell
cmd /c "git add -A && git commit -m \"Mein Commit-Text\" && git push"
```

3) Reiner PowerShell-Workflow (ohne cmd), mit kontrolliertem Ablauf (nur bei Erfolg weitermachen):

```powershell
git add -A
if ($LASTEXITCODE -eq 0) {
  git commit -m "Mein Commit-Text"
  if ($LASTEXITCODE -eq 0) {
    git push
  }
}
```

4) Was man vermeiden sollte
- Direktes Verwenden von `&&` in PowerShell ohne `cmd /c` oder entsprechende Prüfung führt zu Parser-Fehlern.
- Unsaubere Quoting-Mischungen (z. B. doppelte Quotes innen ohne Escape) führen zu Fehlern.

5) Quick checks nach dem Commit

```cmd
git status
git log --oneline -n 5
```

6) Hinweis
- Ich werde mir diese Regeln merken und bei künftigen Commit/Push-Anfragen auf Windows explizit die passende Form (cmd vs PowerShell) wählen.

Datei: docs/GIT_WINDOWS.md

