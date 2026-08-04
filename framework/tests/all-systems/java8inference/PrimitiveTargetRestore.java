// An invocation in which one inference variable is instantiated to a primitive type and a later
// variable can only be resolved with capture.  Before resolving with capture, inference restores
// the bounds that were saved before the failed attempt to resolve without capture; the restore
// must recover the boxed instantiation of the first variable.  See JLS 18.4.

public class PrimitiveTargetRestore {

  // `U` is resolved first, to the primitive target type (which is boxed).  `T` cannot be resolved
  // without capture, because its only upper bound, `Enum<T>`, is not a proper type.
  <U, T extends Enum<T>> U uninstantiated() {
    throw new AssertionError();
  }

  // Each call independently exercises the restore, for a different primitive type: `U` is
  // instantiated to `int` (boxed to `Integer`) and to `char` (boxed to `Character`).
  void method() {
    int i = uninstantiated();
    char c = uninstantiated();
  }
}
