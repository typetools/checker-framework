// Tests the case in which applying every default in one traversal of the type would NOT give the
// same result as traversing the type once per default.
//
// The precedence list for a method of this class is
//     [ @SubQual ALL ]  ++  [ ..., @SuperQual RETURN, ... ]
// @SubQual ALL annotates nodes below the top level of the type.  @SuperQual RETURN annotates the
// return type from the top-level executable node.  So whether @SubQual or @SuperQual reaches the
// return type first depends on whether defaults are applied one node at a time or one default at
// a time, and QualifierDefaults must fall back to one traversal per default here.
//
// Applying all the defaults in one traversal would instead make the return type @SuperQual, and
// the assignment below would not type-check.

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

@DefaultQualifier(value = SubQual.class, locations = TypeUseLocation.ALL)
public class SinglePassFallback {

  // Declared explicitly, so that the default does not give the constructor a @SubQual result
  // while Object's constructor has a @SuperQual result.
  @SuperQual SinglePassFallback() {}

  @SubQual Object subField;

  Object returnsSub() {
    return subField;
  }

  void useReturn() {
    @SubQual Object ok = returnsSub();
  }
}
