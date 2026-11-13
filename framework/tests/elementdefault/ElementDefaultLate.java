// Tests that a default registered through QualifierDefaults.addElementDefault applies, even if the
// defaults within the element were already computed when the default was registered.
//
// ElementDefaultAnnotatedTypeFactory registers @SubQual for RETURN on the class below, computes
// the type of method first(), and then registers @SubQual for PARAMETER.

import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

public class ElementDefaultLate {

  @SubQual Object subField;

  Object first() {
    return subField;
  }

  // The return type is @SubQual, from the default that was registered before the type of first()
  // was computed.
  void useReturn() {
    @SubQual Object sub = first();
  }

  // The parameter is @SubQual, from the default that was registered after the type of first()
  // was computed.
  void second(Object param) {}

  void useParam(@SubQual Object sub, @SuperQual Object sup) {
    second(sub);
    // :: error: (argument)
    second(sup);
  }
}
