import org.checkerframework.checker.index.qual.IndexFor;

public class IndexForVarargs {
  String get(@IndexFor("#2") int i, String... varargs) {
    return varargs[i];
  }

  void method(@IndexFor("#2") int i, String[]... varargs) {}

  void m() {
    // :: error: (argument.type.incompatible)
    get(1);

    get(1, "a", "b");

    // :: error: (argument.type.incompatible)
    get(2, "abc");

    String[] stringArg1 = new String[] {"a", "b"};
    String[] stringArg2 = new String[] {"c", "d", "e"};
    String[] stringArg3 = new String[] {"a", "b", "c"};

    method(1, stringArg1, stringArg2);

    // :: error: (argument.type.incompatible)
    method(2, stringArg3);

    get(1, stringArg1);

    // :: error: (argument.type.incompatible)
    get(3, stringArg2);
  }
}
