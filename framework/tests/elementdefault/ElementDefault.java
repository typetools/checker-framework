// Tests that a default registered through QualifierDefaults.addElementDefault composes with the
// @DefaultQualifier annotations on the same element and on enclosing scopes, rather than
// suppressing them.
//
// ElementDefaultAnnotatedTypeFactory registers @SubQual for RETURN on the class below.

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

@DefaultQualifier(value = SubQual.class, locations = TypeUseLocation.PARAMETER)
public class ElementDefault {

  @SubQual Object subField;

  Object unannotatedField;

  // The return type is @SubQual, from the default that addElementDefault registered for this
  // class.
  Object returnsSub() {
    return subField;
  }

  void useReturn() {
    @SubQual Object ok = returnsSub();
  }

  // The parameter is @SubQual, from the @DefaultQualifier on this class.  If the element
  // default suppressed the @DefaultQualifier, as it did before defaultsAt composed the two,
  // the parameter would be @SuperQual instead, from @DefaultQualifierInHierarchy on SuperQual,
  // and the second call below would type-check.
  void takesSub(Object param) {}

  void useParam(@SubQual Object sub, @SuperQual Object sup) {
    takesSub(sub);
    // :: error: (argument)
    takesSub(sup);
  }

  // A field is neither RETURN nor PARAMETER, so a field still falls back to @SuperQual, which
  // shows that the element default does not widen to locations it does not name.
  void fieldFallsBack() {
    // :: error: (assignment)
    @SubQual Object bad = unannotatedField;
  }
}
