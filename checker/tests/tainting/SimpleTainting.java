import org.checkerframework.checker.tainting.qual.Untainted;

public class SimpleTainting {

  void execute(@Untainted String s) {}

  void tainted(String s) {}

  void stringLiteral() {
    execute("ldskjfldj");
    tainted("lksjdflkjdf");
  }

  void stringRef(String ref) {
    // :: error: (argument)
    execute(ref); // error
    tainted(ref);
  }

  void untaintedRef(@Untainted String ref) {
    execute(ref);
    tainted(ref);
  }

  void concatenation(@Untainted String s1, String s2) {
    execute(s1 + s1);
    execute(s1 += s1);
    execute(s1 + "m");
    // :: error: (argument)
    execute(s1 + s2); // error

    // :: error: (argument)
    execute(s2 + s1); // error
    // :: error: (argument)
    execute(s2 + "m"); // error
    // :: error: (argument)
    execute(s2 + s2); // error

    tainted(s1 + s1);
    tainted(s1 + "m");
    tainted(s1 + s2);

    tainted(s2 + s1);
    tainted(s2 + "m");
    tainted(s2 + s2);
  }
}
