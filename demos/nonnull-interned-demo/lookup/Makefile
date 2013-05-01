JAVAC=../lib/javac.jar
CHECKERS=../lib/checkers.jar

lookup:
	javac -cp $(CHECKERS) -processor checkers.nullness.NullnessChecker -sourcepath src:jdk/src src/utilMDE/UtilMDE.java
