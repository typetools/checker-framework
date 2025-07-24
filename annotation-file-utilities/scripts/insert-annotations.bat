
:: Insert annotations (from an annoation file) into a class file.
:: For usage information, run: insert-annotations.bat --help
:: See the annotation file utilities documentation for more information.
:: This only works for Java 9+.

set ANNOTATION_FILE_UTILS=%~d0
set ANNOTATION_FILE_UTILS=%ANNOTATION_FILE_UTILS%%~p0
set ANNOTATION_FILE_UTILS=%ANNOTATION_FILE_UTILS%\..\annotation-file-utilities-all.jar
set JAVAC_JAR=%ANNOTATION_FILE_UTILS%\annotation-file-utilities\lib\javac-9+181-r4173-1.jar

java -ea -cp "%JAVAC_JAR%;%ANNOTATION_FILE_UTILS%;%CLASSPATH%" ClassFileWriter %*
