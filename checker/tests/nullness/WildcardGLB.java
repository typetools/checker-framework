import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class WildcardGLB {

  static class MyClass<E extends List<@Nullable String>> {
    E getE() {
      throw new RuntimeException();
    }
  }

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
  // :: error: (type.argument)
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
  // :: error: (type.argument)
  void use3(MyClass2<? extends List<String>> s) {
    // :: error: (assignment)
    List<String> f = s.getE();
    List<@Nullable String> f2 = s.getE();
  }

  // :: error: (type.argument)
  void use4(MyClass2<? extends ArrayList<String>> s) {
    // :: error: assignment
    List<String> f = s.getE();
    List<@Nullable String> f2 = s.getE(); // ok
  }
}
