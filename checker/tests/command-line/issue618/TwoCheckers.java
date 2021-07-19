// Expected error appears:
// $ch/bin/javac -processor org.checkerframework.checker.tainting.TaintingChecker
// TwoCheckers.java
// $ch/bin/javac -processor org.checkerframework.checker.tainting.TaintingChecker,regex
// TwoCheckers.java

// Expected error is suppressed:
// $ch/bin/javac -processor regex,org.checkerframework.checker.tainting.TaintingChecker
// TwoCheckers.java

// Compare these two executions:
// $ch/bin/javac -processor org.checkerframework.checker.tainting.TaintingChecker,regex
// TwoCheckers.java -AprintAllQualifiers -Ashowchecks > out-good.txt
// $ch/bin/javac -processor regex,org.checkerframework.checker.tainting.TaintingChecker
// TwoCheckers.java -AprintAllQualifiers -Ashowchecks > out-bad.txt

// Turning off caches has no effect:
// $ch/bin/javac -processor regex,org.checkerframework.checker.tainting.TaintingChecker
// TwoCheckers.java -AprintAllQualifiers -Ashowchecks -AatfDoNotCache AAatfDoNotReadCache >
// out-bad-nocache.txt

import org.checkerframework.checker.tainting.qual.Untainted;

public class TwoCheckers {

  void client(String a) {
    // :: error: (argument)
    requiresUntainted(a);
  }

  void requiresUntainted(@Untainted String b) {}
}
