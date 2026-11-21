import org.checkerframework.checker.interning.qual.Interned;

// @below-java21-jdk-skip-test

// A binding variable nested in a record pattern keeps the type-based default of its type, such as
// @Interned for a class annotated @Interned.  The binding variable is @Interned even if the record
// component is not, because @Interned is the qualifier upper bound of the declaration of Token.
public class NestedBindingVariableDefault {

  @Interned static class Token {}

  static class NotInterned {}

  record Box(Token t, NotInterned n) {}

  record ObjectBox(Object o) {}

  void objectComponent(Object o) {
    if (o instanceof ObjectBox(Token t)) {
      take(t);
    }
    switch (o) {
      case ObjectBox(Token t) -> take(t);
      default -> {}
    }
  }

  void take(@Interned Token t) {}

  void takeNotInterned(@Interned NotInterned n) {}

  void caseLabel(Object o) {
    switch (o) {
      case Box(Token t, NotInterned n) -> {
        take(t);
        // :: error: [argument]
        takeNotInterned(n);
      }
      default -> {}
    }
  }

  void instanceOf(Object o) {
    if (o instanceof Box(Token t, NotInterned n)) {
      take(t);
    }
  }
}
