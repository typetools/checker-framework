import org.checkerframework.checker.tainting.qual.Untainted;

public class SimplePrims {

  void execute(@Untainted int s) {}

  void tainted(int s) {}

  void intLiteral() {
    // :: error: (argument)
    execute(5);
    tainted(6);
  }

  void intRef(int ref) {
    // :: error: (argument)
    execute(ref);
    tainted(ref);
  }

  void untaintedRef(@Untainted int ref) {
    execute(ref);
    tainted(ref);
  }

  void concatenation(@Untainted int s1, int s2) {
    execute(s1 + s1);
    execute(s1 += s1);
    // :: error: (argument)
    execute(s1 + 3);

    // :: error: (argument)
    execute(s1 + s2);

    // :: error: (argument)
    execute(s2 + s1);
    // :: error: (argument)
    execute(s2 + 4);
    // :: error: (argument)
    execute(s2 + s2);

    tainted(s1 + s1);
    tainted(s1 + 7);
    tainted(s1 + s2);

    tainted(s2 + s1);
    tainted(s2 + 8);
    tainted(s2 + s2);
  }
}
