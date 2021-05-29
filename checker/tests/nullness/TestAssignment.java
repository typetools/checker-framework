package examples;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TestAssignment {

  void a() {
    @NonNull String f = "abc";

    // :: error: (assignment)
    f = null;
  }

  void b() {
    @UnknownInitialization @NonNull TestAssignment f = new TestAssignment();
  }
}
