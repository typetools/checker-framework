// Tests a checker whose applier overrides addAnnotation, so that applying a default does not mean
// "add the qualifier if the hierarchy is empty".
//
// The precedence list for a method of this class is
//     [ @SubQual RETURN ]  ++  [ ..., @CustomApplierBottom RETURN, ... ]
// Under the standard applier the second of those is redundant, because the first one fills the
// hierarchy.  This applier ignores @SubQual, so the second one is what annotates the return type
// and QualifierDefaults must not discard it.
//
// If it were discarded, no default would apply to the return type at RETURN, the OTHERWISE default
// would make the return type @SuperQual, and the assignment below would not type-check.

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.customapplier.CustomApplierBottom;
import org.checkerframework.framework.testchecker.util.SubQual;

@DefaultQualifier(value = SubQual.class, locations = TypeUseLocation.RETURN)
public class CustomApplier {

  @CustomApplierBottom Object bottomField;

  Object returnsBottom() {
    return bottomField;
  }

  void useReturn() {
    @CustomApplierBottom Object ok = returnsBottom();
  }
}
