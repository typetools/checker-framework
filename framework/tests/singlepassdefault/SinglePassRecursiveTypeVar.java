// Tests a local variable whose type is a recursive type variable, to which defaults apply because
// local type variables are defaulted when flow-sensitive refinement is enabled.
//
// The precedence list for method `use` is
//     [ @SuperQual RETURN, @SuperQual UPPER_BOUND ]  ++  [ @SubQual LOCAL_VARIABLE ]  ++  ...
// (The method-level RETURN default makes the checked code default for RETURN redundant, so no
// cross-node default follows UPPER_BOUND.)
//
// The type of `x` is the type variable T, and T's upper bound Comparable<T> contains the very same
// T node.  When the type is traversed once per default, the UPPER_BOUND traversal reaches T inside
// its own upper bound and annotates it @SuperQual before the LOCAL_VARIABLE traversal runs.  So x's
// declared type is @SuperQual T, and the assignment below type-checks.  Applying all the defaults
// in one traversal would instead apply @SubQual LOCAL_VARIABLE to T first, and the assignment would
// not type-check.

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

@DefaultQualifier(value = SubQual.class, locations = TypeUseLocation.LOCAL_VARIABLE)
public class SinglePassRecursiveTypeVar {

  @DefaultQualifier(
      value = SuperQual.class,
      locations = {TypeUseLocation.RETURN, TypeUseLocation.UPPER_BOUND})
  <T extends Comparable<T>> void use(T p) {
    T x = p;
  }
}
