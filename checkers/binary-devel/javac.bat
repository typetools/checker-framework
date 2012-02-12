@ECHO OFF

REM  This is a Windows Batch script based on Sun's javac bash script

SETLOCAL

REM Note that mydir ends with a '\'
SET mydir=%~dp0
SET cfroot=%mydir%..
SET jsr308root=%cfroot%/../../jsr308-langtools
SET javac-jar=%mydir%jsr308-all.jar

SET bcp="%cfroot%/build";"%cfroot%/../javaparser/build";"%jsr308root%/build/classes";"%cfroot%/binary/jdk.jar";"%cfroot%/binary/jsr308-all.jar"

java -Xbootclasspath/p:%bcp% -ea:com.sun.tools... com.sun.tools.javac.Main -classpath '.' %*

