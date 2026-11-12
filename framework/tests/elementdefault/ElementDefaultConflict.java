// Tests that a @DefaultQualifier written on an element takes precedence over a default that
// QualifierDefaults.addElementDefault registered for the same element, location, and hierarchy.
//
// ElementDefaultAnnotatedTypeFactory registers @SubQual for RETURN on the class below.  It also
// registers @Odd and then @SubQual for PARAMETER; the type system does not support @Odd, so
// @Odd is ignored.

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

@DefaultQualifier(value = SuperQual.class, locations = TypeUseLocation.RETURN)
public class ElementDefaultConflict {

  @SuperQual Object superField;

  // The return type is @SuperQual, from the @DefaultQualifier on this class, rather than
  // @SubQual, from the registered default.
  Object returnsSuper() {
    return superField;
  }

  void useReturn() {
    // :: error: (assignment)
    @SubQual Object bad = returnsSuper();
  }

  // The parameter is @SubQual, from the registered default.
  void takesSub(Object param) {}

  void useParam(@SubQual Object sub, @SuperQual Object sup) {
    takesSub(sub);
    // :: error: (argument)
    takesSub(sup);
  }
}
