@echo off
REM Push script for MinecraftSurvivors
REM Usage: run this after Git is installed and you're in Windows (cmd.exe)

cd /d "%~dp0.."

REM Check for git availability
where git >nul 2>&1
if %ERRORLEVEL% neq 0 (
  echo ERROR: Git wurde auf diesem System nicht gefunden.
  echo Bitte installiere Git for Windows: https://git-scm.com/download/win
  echo Waehle waehrend der Installation die Option "Use Git from the Windows Command Prompt" oder stelle sicher, dass git in PATH ist.
  echo Nach der Installation die Shell neu starten und erneut ausfuehren.
  pause
  exit /b 1
)

echo === Initializing repository (if needed) ===
if not exist .git (
  git init
) else (
  echo .git already exists
)

REM remove existing origin to avoid errors (if present)
git remote remove origin 2>nul || echo no remote to remove

echo === Adding remote origin ===
git remote add origin https://github.com/bySenom/Minecraft-Survivors.git || echo remote add failed (may already exist)

echo === Staging files ===
git add -A

echo === Committing ===
git commit -m "Add README and update docs links" || echo No changes to commit

echo === Ensuring 'main' branch ===
git branch -M main

echo === Pushing to origin main ===
git push -u origin main

if %ERRORLEVEL% neq 0 (
  echo.
  echo Push failed. If prompted for credentials, use your GitHub username and a Personal Access Token (PAT) as password.
  echo To avoid prompting, configure SSH or a credential manager (Git Credential Manager is bundled with Git for Windows).
) else (
  echo Push completed successfully.
)

REM Removed interactive pause to avoid issues when running from PowerShell/CI
echo Push script finished.
