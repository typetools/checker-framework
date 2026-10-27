// An exact method reference whose compile-time declaration has primitive parameters does not
// make the inferred type argument's qualifiers equal to those of the primitive type.

public class ExactMethodRefPrimitiveParameter {
  interface DefaultAction<R, P> {
    R defaultAction(Object type, P p);
  }

  interface Reduce<R> {
    R reduce(R r1, R r2);
  }

  static class Scanner<R, P> {
    Scanner(DefaultAction<R, P> defaultAction, Reduce<R> reduce, R defaultResult) {}
  }

  Scanner<Boolean, Void> scanner = new Scanner<>((t, p) -> true, Boolean::logicalOr, false);

  static <R> R reduce(Reduce<R> reduce, R defaultResult) {
    return defaultResult;
  }

  Boolean b = reduce(Boolean::logicalOr, false);

  Integer i = reduce(Integer::sum, 0);
}
