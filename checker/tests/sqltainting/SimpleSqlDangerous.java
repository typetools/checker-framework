import org.checkerframework.checker.sqltainting.qual.SqlSafe;

public class SimpleSqlDangerous {

  void execute(@SqlSafe String s) {}

  void dangerous(String s) {}

  void stringLiteral() {
    execute("ldskjfldj");
    dangerous("lksjdflkjdf");
  }

  void stringRef(String ref) {
    // :: error: (argument)
    execute(ref); // error
    dangerous(ref);
  }

  void safeRef(@SqlSafe String ref) {
    execute(ref);
    dangerous(ref);
  }

  void concatenation(@SqlSafe String s1, String s2) {
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

    dangerous(s1 + s1);
    dangerous(s1 + "m");
    dangerous(s1 + s2);

    dangerous(s2 + s1);
    dangerous(s2 + "m");
    dangerous(s2 + s2);
  }
}
