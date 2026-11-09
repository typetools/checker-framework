import org.checkerframework.checker.interning.qual.Interned;

// @below-java21-jdk-skip-test

// Every Class is @Interned, so a binding variable of type Class is @Interned, as a cast to Class
// is.
public class BindingVariableClass {

  void take(@Interned Class<?> c) {}

  void cast(Object value) {
    take((Class<?>) value);
  }

  void instanceOfPattern(Object value) {
    if (value instanceof Class<?> cls) {
      take(cls);
    }
  }

  void caseLabelPattern(Object value) {
    switch (value) {
      case Class<?> cls -> take(cls);
      default -> {}
    }
  }

  void notInterned(Object value) {
    if (value instanceof String s) {
      // :: error: (argument)
      takeString(s);
    }
  }

  void takeString(@Interned String s) {}
}
