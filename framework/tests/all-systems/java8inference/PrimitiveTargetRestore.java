// An invocation in which one inference variable is instantiated to a primitive type and a later
// variable can only be resolved with capture.  Resolving with capture first restores the bounds
// that were saved before the failed attempt to resolve without capture, which must recover the
// boxed instantiation of the first variable.  See JLS 18.4.

public class PrimitiveTargetRestore {

  // `u` is resolved first, to `int` (which is boxed to `Integer`).  `t` cannot be resolved
  // without capture, because its only upper bound, `Enum<T>`, is not a proper type.
  <U, T extends Enum<T>> U uninstantiated() {
    throw new AssertionError();
  }

  void method() {
    int i = uninstantiated();
    char c = uninstantiated();
  }
}
