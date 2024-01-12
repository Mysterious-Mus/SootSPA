#!/usr/bin/env pwsh

# Check javac
$javac = 'javac'
if ($env:JAVA_HOME) {
    $javac = Join-Path $env:JAVA_HOME 'bin/javac'
    Write-Host "Using javac from JAVA_HOME: $javac"
}
if (!(Get-Command $javac -ErrorAction SilentlyContinue)) {
    throw "JAVA_HOME is not set or javac is not in the path. `
Please set JAVA_HOME to the location of your JDK or add javac to the path"
}

# Compile all .java files in subdirectories
Get-ChildItem -Directory | Foreach-Object {
    Push-Location $_
    Remove-Item -Force -Path "*.class"
    Write-Host "Compiling .java files in directory: $($_.FullName)"
    $items = Get-ChildItem -Path '*.java'
    if ($items) { 
        & $javac -g -Xlint:deprecation -Xlint:unchecked @items 2>&1 | Write-Host 
    }
    Pop-Location
}