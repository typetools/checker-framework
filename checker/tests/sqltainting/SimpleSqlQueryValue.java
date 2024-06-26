import org.checkerframework.checker.sqltainting.qual.SqlSanitized;

public class SimpleSqlQueryValue {

  void execute(@SqlSanitized String s) {}

  void unsanitized(String s) {}

  void stringLiteral() {
    execute("ldskjfldj");
    unsanitized("lksjdflkjdf");
  }

  void stringRef(String ref) {
    // :: error: (argument)
    execute(ref); // error

    unsanitized(ref);
  }

  void safeRef(@SqlSanitized String ref) {
    execute(ref);
    unsanitized(ref);
  }

  void concatenation(@SqlSanitized String s1, String s2) {
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

    unsanitized(s1 + s1);
    unsanitized(s1 + "m");
    unsanitized(s1 + s2);

    unsanitized(s2 + s1);
    unsanitized(s2 + "m");
    unsanitized(s2 + s2);
  }
}
