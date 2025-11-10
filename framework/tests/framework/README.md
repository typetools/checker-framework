# Framework tests (`framework/` subdirectory)

Java files in this directory are allowed to contain Java errors
(that is, to cause javac without a processor to issue an error).
This is an exception to the rules in ../../../checker/tests/README.md .

To run the tests, do

```sh
  cd $CHECKERFRAMEWORK/framework
  ../gradlew FrameworkTest
```

To run a single test, do something like:

<!-- markdownlint-disable line-length -->
```sh
  cd $CHECKERFRAMEWORK/framework/tests/framework
  (cd $CHECKERFRAMEWORK && ./gradlew assemble :framework:compileTestJava) && javacheck -processor org.checkerframework.framework.testchecker.util.H1H2Checker -cp $CHECKERFRAMEWORK/framework/build/classes/java/test/
```
<!-- markdownlint-enable line-length -->
