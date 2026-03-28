
:: Extract annotations from a class file and write them to an annotation file.
:: For usage information, run: extract-annotations.bat --help
:: See the annotation file utilities documentation for more information.
:: This only works with Java 9+.
set ANNOTATION_FILE_UTILS=%~d0
set ANNOTATION_FILE_UTILS=%ANNOTATION_FILE_UTILS%%~p0
set ANNOTATION_FILE_UTILS=%ANNOTATION_FILE_UTILS%\..\dist\annotation-file-utilities-all.jar

set JDK_OPENS=--add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-opens jdk.internal.opt/jdk.internal.opt=ALL-UNNAMED
java -ea %JDK_OPENS% -cp "%ANNOTATION_FILE_UTILS%;%CLASSPATH%" ClassFileReader %*

