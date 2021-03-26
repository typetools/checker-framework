public class UnannoPrimitivesDefaults {
  @org.checkerframework.framework.qual.DefaultQualifier(
      org.checkerframework.checker.nullness.qual.NonNull.class)
  class Decl {
    // The return type is not annotated with @NonNull, because
    // the implicit annotation for @Primitive takes precedence.
    int test() {
      return 5;
    }
  }

  class Use {
    Decl d = new Decl();
    int x = d.test();
  }
}
