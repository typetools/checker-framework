import org.checkerframework.checker.nullness.qual.*;

import java.util.Map;

@SuppressWarnings("fields.uninitialized")
public class KeyForValidation {

  //:: error: (keyfor.type.invalid)
  static @KeyFor("this") Object f;

  @KeyFor("this") Object g;

  // TODO: invalid index (it's a common mistake to assume indexing is zero-based)
  void m(@KeyFor("#0") Object p) {}

  // TODO: invalid index
  void m2(@KeyFor("#4") Object p) {}

  // OK
  void m3(@KeyFor("#2") Object p, Map m) {}

  // TODO: index for a non-map
  void m4(@KeyFor("#1") Object p, Map m) {}

  // TODO: index with wrong type
  void m4(@KeyFor("#2") String p, Map<Integer,Integer> m) {}

  // TODO: check names for fields
  @KeyFor("INVALID") Object h;

  @KeyFor("f") Object i;

  void foo(Object p) {
    // TODO: check names for fields or local variables (also parameter names?)
    @KeyFor("ALSOBAD") Object j;

    @KeyFor("j") Object k;
    @KeyFor("f") Object l;

    // TODO: should we allow this or should it be #1?
    @KeyFor("p") Object o;
  }
}
