import org.checkerframework.checker.nullness.qual.*;

interface FunctionAC {
  String apply(String s);
}

interface FunctionAC2 {
  String apply(@Nullable String s);
}

public class AssignmentContextTest {
  // Test assign
  FunctionAC f1 = String::toString;
  // :: error: (methodref.receiver)
  FunctionAC2 f2 = String::toString;

  // Test casts
  Object o1 = (Object) (FunctionAC) String::toString;
  // :: error: (methodref.receiver)
  Object o2 = (Object) (FunctionAC2) String::toString;

  void take(FunctionAC f) {
    // Test argument assingment
    take(String::toString);
  }

  void take2(FunctionAC2 f) {
    // Test argument assingment
    // :: error: (methodref.receiver)
    take2(String::toString);
  }

  FunctionAC supply() {
    // Test return assingment
    return String::toString;
  }

  FunctionAC2 supply2() {
    // Test return assingment
    // :: error: (methodref.receiver)
    return String::toString;
  }
}
