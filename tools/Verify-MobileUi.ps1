param([string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot))
$ErrorActionPreference = 'Stop'
$resourceRoot = Join-Path $ProjectRoot 'app/src/main/res'
$androidNamespace = 'http://schemas.android.com/apk/res/android'
$failures = [System.Collections.Generic.List[string]]::new()
$ids = [System.Collections.Generic.HashSet[string]]::new()
$resources = [System.Collections.Generic.HashSet[string]]::new()
$xmlFiles = Get-ChildItem -LiteralPath $resourceRoot -Recurse -Filter '*.xml'
foreach ($file in $xmlFiles) {
    try { [xml]$doc = Get-Content -Raw -LiteralPath $file.FullName }
    catch { $failures.Add("Malformed XML: $($file.FullName)"); continue }
    $folder = $file.Directory.Name.Split('-')[0]
    if ($folder -eq 'values') {
        foreach ($entry in $doc.resources.ChildNodes) {
            if ($entry.NodeType -ne 'Element') { continue }
            $type = if ($entry.LocalName -eq 'item') { $entry.GetAttribute('type') } else { $entry.LocalName }
            [void]$resources.Add("$type/$($entry.GetAttribute('name'))")
        }
    } else { [void]$resources.Add("$folder/$($file.BaseName)") }
    foreach ($element in $doc.SelectNodes('//*')) {
        $idValue = $element.GetAttribute('id', $androidNamespace)
        if ($idValue.StartsWith('@+id/')) { [void]$ids.Add($idValue.Substring(5)) }
        if ($folder -eq 'layout' -and $element.LocalName -notin @('include', 'merge', 'requestFocus', 'tag')) {
            foreach ($dimension in @('layout_width', 'layout_height')) {
                if (!$element.HasAttribute($dimension, $androidNamespace)) {
                    $failures.Add("Missing $dimension in $($file.Name): $($element.Name) $idValue")
                }
            }
        }
    }
}
Get-ChildItem -LiteralPath $resourceRoot -Recurse -File | Where-Object Extension -ne '.xml' | ForEach-Object {
    [void]$resources.Add("$($_.Directory.Name.Split('-')[0])/$($_.BaseName)")
}
foreach ($id in $ids) { [void]$resources.Add("id/$id") }
foreach ($file in $xmlFiles) {
    $raw = [regex]::Replace((Get-Content -Raw -LiteralPath $file.FullName), '(?s)<!--.*?-->', '')
    foreach ($match in [regex]::Matches($raw, '@(?:\+)?([a-z]+)/([A-Za-z0-9_.]+)')) {
        $key = "$($match.Groups[1].Value)/$($match.Groups[2].Value)"
        if (!$resources.Contains($key)) { $failures.Add("Unresolved local resource $key in $($file.Name)") }
    }
}
$javaRoot = Join-Path $ProjectRoot 'app/src/main/java'
foreach ($file in Get-ChildItem -LiteralPath $javaRoot -Recurse -Filter '*.java') {
    $raw = Get-Content -Raw -LiteralPath $file.FullName
    foreach ($match in [regex]::Matches($raw, '(?<![.\w])R\.(id|layout|drawable|color|string)\.([A-Za-z0-9_]+)')) {
        $key = "$($match.Groups[1].Value)/$($match.Groups[2].Value)"
        if (!$resources.Contains($key)) { $failures.Add("Unknown Java resource $key in $($file.Name)") }
    }
}
[xml]$dashboard = Get-Content -Raw -LiteralPath (Join-Path $resourceRoot 'layout/fragment_dashboard.xml')
$overlay = @($dashboard.SelectNodes('//*') | Where-Object { $_.GetAttribute('id', $androidNamespace) -eq '@+id/progress_overlay' })
if ($overlay.Count -ne 1 -or $overlay[0].LocalName -ne 'FrameLayout') { $failures.Add('Dashboard overlay must be a FrameLayout.') }
$fragment = Get-Content -Raw -LiteralPath (Join-Path $javaRoot 'com/myfarmio/app/ui/dashboard/DashboardFragment.java')
if ($fragment -notmatch 'FrameLayout progressOverlay') { $failures.Add('Dashboard Java overlay type must match XML.') }
[xml]$navigation = Get-Content -Raw -LiteralPath (Join-Path $resourceRoot 'navigation/nav_main_graph.xml')
foreach ($route in $navigation.navigation.fragment) {
    $name = $route.GetAttribute('name', $androidNamespace)
    $javaFile = Join-Path $javaRoot ($name.Replace('.', '/') + '.java')
    if (!(Test-Path -LiteralPath $javaFile)) { $failures.Add("Missing route class: $name") }
}
if ($failures.Count -gt 0) { $failures | ForEach-Object { Write-Error $_ -ErrorAction Continue }; exit 1 }
Write-Output "PASS: $($xmlFiles.Count) XML files; layout dimensions, local references, navigation classes and Dashboard overlay contract."
Write-Output 'This static check does not replace Android resource linking, instrumentation or visual verification.'
