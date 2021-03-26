import org.checkerframework.checker.interning.qual.Interned;

public class Creation2 {

  @Interned class Baz {
    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @Interned Baz() {}
  }

  void test() {
    Baz b = new Baz();
  }
}
