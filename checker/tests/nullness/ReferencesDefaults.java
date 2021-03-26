public class ReferencesDefaults {
  @org.checkerframework.framework.qual.DefaultQualifier(
      org.checkerframework.checker.nullness.qual.Nullable.class)
  class Decl {
    Object test() {
      // legal, because of changed default.
      return null;
    }
  }

  class Use {
    Decl d = new Decl();
    // here the default for f is NonNull -> error
    // :: error: (assignment.type.incompatible)
    Object f = d.test();
  }
}
