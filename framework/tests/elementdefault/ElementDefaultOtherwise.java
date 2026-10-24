// Tests that a default for a specific location takes precedence over a default for OTHERWISE, even
// when the OTHERWISE default belongs to a nearer scope or was registered through
// QualifierDefaults.addElementDefault.  OTHERWISE means "apply if nothing more concrete is
// provided".
//
// ElementDefaultAnnotatedTypeFactory registers @ElementDefaultQual for OTHERWISE on class
// ElementDefaultRegisteredOtherwise below.
//
// If the OTHERWISE default won, then each return type would be @ElementDefaultQual rather than
// @SubQual, and the return statements and the assignments below would not type-check.

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultQual;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

@DefaultQualifier(value = SubQual.class, locations = TypeUseLocation.RETURN)
public class ElementDefaultOtherwise {

  static @SubQual Object sub;

  // The return type is @SubQual, from the RETURN default on the enclosing class.
  @DefaultQualifier(value = ElementDefaultQual.class, locations = TypeUseLocation.OTHERWISE)
  static Object returnsSub() {
    return sub;
  }

  static void useReturn() {
    @SubQual Object ok = returnsSub();
    // :: error: (assignment)
    @ElementDefaultQual Object bad = returnsSub();
  }
}

class ElementDefaultRegisteredOtherwise {

  // Without an explicit qualifier, the registered OTHERWISE default would make the constructor
  // result @ElementDefaultQual, which is not the type of super().
  @SuperQual ElementDefaultRegisteredOtherwise() {}

  static @SubQual Object sub;

  // The return type is @SubQual, from the RETURN default on this method, not @ElementDefaultQual
  // from the registered OTHERWISE default on the enclosing class.
  @DefaultQualifier(value = SubQual.class, locations = TypeUseLocation.RETURN)
  static Object returnsSub() {
    return sub;
  }

  static void useReturn() {
    @SubQual Object ok = returnsSub();
    // :: error: (assignment)
    @ElementDefaultQual Object bad = returnsSub();
  }
}
