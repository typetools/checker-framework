import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class WildcardGLB {

  static class MyClass<E extends List<@Nullable String>> {
    E getE() {
      throw new RuntimeException();
    }
  }

  // TODO: The formal parameter type is an invalid type, so the CF should issue an error about it.
  // The captured type variable for
  //    ? extends @List<@NonNull String>
  // is
  //    capture#865 extends @NonNull List<@Nullable String>
  // .  The upper bound of
  // the captured type variable is not a subtype of the extends bound of the
  // wildcard because the glb of the type parameter bound and the wildcard
  // extends bound does not exist.  I don't think this leads to unsoundness,
  // but it makes it so that this method can't be called without an error.  The
  // method testUse below demos this.
  void use(MyClass<? extends List<String>> s) {
    // :: error: (assignment)
    List<String> f = s.getE();
    List<@Nullable String> f2 = s.getE();
  }

  void testUse(
      // :: error: (type.argument)
      MyClass<List<String>> p1,
      // A comment to force a line break.
      MyClass<List<@Nullable String>> p2) {
    use(p1);
    // :: error: (argument)
    use(p2);
  }

  // capture#196 extends @NonNull ArrayList<@NonNull String>
  // :: error: (type.argument)
  void use2(MyClass<? extends ArrayList<String>> s) { // error: type.argument
    List<String> f = s.getE();
    // :: error: (assignment)
    List<@Nullable String> f2 = s.getE(); // error: assignment
  }

  static class MyClass2<E extends ArrayList<@Nullable String>> {
    E getE() {
      throw new RuntimeException();
    }
  }

  // capture#952 extends @NonNull ArrayList<@Nullable String>
  void use3(MyClass2<? extends List<String>> s) {
    // :: error: (assignment)
    List<String> f = s.getE();
    List<@Nullable String> f2 = s.getE();
  }

  // TODO: error here for the same reason as use1.
  // capture#196 extends @NonNull ArrayList<@NonNull String>
  void use4(MyClass2<? extends ArrayList<String>> s) {
    // :: error: assignment
    List<String> f = s.getE();
    List<@Nullable String> f2 = s.getE(); // ok
  }
}
