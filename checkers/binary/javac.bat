@ECHO OFF

REM  This is a Windows Batch script based on Sun's javac bash script

SETLOCAL

REM Note that mydir ends with a '\'
SET mydir=%~dp0
SET javac-jar=%mydir%jsr308-all.jar

java -Xbootclasspath/p:"%javac-jar%" -jar "%javac-jar%" %*
