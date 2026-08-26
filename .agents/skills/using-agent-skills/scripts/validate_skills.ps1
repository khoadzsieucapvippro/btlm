param (
    [string]$SkillsDir = (Join-Path $PSScriptRoot "..\.."),
    [string]$RulesDir = (Join-Path $PSScriptRoot "..\..\..\rules")
)

$hasErrors = $false
$skills = Get-ChildItem -Path $SkillsDir -Directory

foreach ($skill in $skills) {
    $skillFile = Join-Path $skill.FullName "SKILL.md"
    if (-Not (Test-Path $skillFile)) {
        Write-Host "[ERROR] Missing SKILL.md in $($skill.Name)" -ForegroundColor Red
        $hasErrors = $true
        continue
    }

    $content = Get-Content $skillFile -Raw
    
    # Check Frontmatter
    if ($content -notmatch '(?s)^---\r?\nname:\s*(.*?)\r?\ndescription:\s*(.*?)\r?\n---') {
        Write-Host "[ERROR] Invalid frontmatter in $($skill.Name)" -ForegroundColor Red
        $hasErrors = $true
    }

    # Check for references
    $refsDir = Join-Path $skill.FullName "references"
    if (Test-Path $refsDir) {
        $refFiles = Get-ChildItem -Path $refsDir -Filter "*.md"
        foreach ($ref in $refFiles) {
            $refName = "references/$($ref.Name)"
            if ($content -notmatch $refName) {
                Write-Host "[WARNING] Orphan reference $refName in $($skill.Name)" -ForegroundColor Yellow
            }
        }
    }
}

if ($hasErrors) {
    Write-Host "Validation FAILED." -ForegroundColor Red
    exit 1
} else {
    Write-Host "Validation PASSED." -ForegroundColor Green
    exit 0
}
