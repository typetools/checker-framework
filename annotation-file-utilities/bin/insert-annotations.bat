
:: Insert annotations (from an annoation file) into a class file.
:: For usage information, run: insert-annotations.bat --help
:: See the annotation file utilities documentation for more information.

set ANNOTATION_FILE_UTILS=%~d0
set ANNOTATION_FILE_UTILS=%ANNOTATION_FILE_UTILS%%~p0
set ANNOTATION_FILE_UTILS=%ANNOTATION_FILE_UTILS%\..\dist\annotation-file-utilities-all.jar

java -ea -cp "%ANNOTATION_FILE_UTILS%;%CLASSPATH%" org.checkerframework.afu.scenelib.io.classfile.ClassFileWriter %*
