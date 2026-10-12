/**
 * Regression test for https://github.com/typetools/checker-framework/issues/3028.
 *
 * <p>Casting Object to a declared type crashed AsSuperVisitor because asSuper(Object, Cast) was
 * called via castedAsSuper in visitTypeArgs without first checking that the erased Java types are
 * in a subtype relationship.
 */
class CastObjectToDeclaredType {
  void cast(Object o) {
    // :: warning: (cast.unsafe)
    CastObjectToDeclaredType cast = (CastObjectToDeclaredType) o;
  }
}
