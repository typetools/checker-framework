import checkers.nullness.quals.*;

@SuppressWarnings("fields.uninitialized")
public class KeyForValidation {

  //:: error: (keyfor.type.invalid)
  static @KeyFor("this") Object f;

  @KeyFor("this") Object g;

  // TODO: invalid index
  void m(@KeyFor("#4") Object p) {}

  // TODO: check names for fields
  @KeyFor("INVALID") Object h;

  @KeyFor("f") Object i;

  void foo(Object p) {
    // TODO: check names for fields or local variables (also parameter names?)
    @KeyFor("ALSOBAD") Object j;

    @KeyFor("j") Object k;
    @KeyFor("f") Object l;

    // TODO: should we allow this or should it be #0?
    @KeyFor("p") Object o;
  }
}
