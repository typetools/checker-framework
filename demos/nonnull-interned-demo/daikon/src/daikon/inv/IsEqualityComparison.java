package daikon.inv;

import utilMDE.*;

/** This Filter returns true if its argument is an Invariant which satisfies
 * the following conditions:
 * <ul>
 *  <li> the Invariant is a Comparison (which reports <, >, =, <=, or >=)
 *  <li> the relationship reported by the comparison is = (not <, <=, >, or >=)
 * </ul>
 * This does not consider PairwiseIntComparison to be an equality invariant.
 **/
public final class IsEqualityComparison implements Filter<Invariant> {

  // Don't create new instances, just use this existing one
  public static final IsEqualityComparison it = new IsEqualityComparison();

  private IsEqualityComparison() { }

  public boolean accept(Invariant inv) {
    if (!(inv instanceof Comparison))
      return false;
    double chance_conf = ((Comparison) inv).eq_confidence();
    return chance_conf > Invariant.dkconfig_confidence_limit;
  }

  // Sadly, this does not work -- it conflicts with the member definition.
  // public static boolean accept(Object o) {
  //   return it.accept(o);
  // }
}
