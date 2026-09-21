// Tests that a default registered through QualifierDefaults.addElementDefault takes precedence
// over a @DefaultQualifier annotation at the same location.
//
// ElementDefaultAnnotatedTypeFactory registers @SubQual for FIELD on the class below, which also
// has a @DefaultQualifier for FIELD.  The name of @ElementDefaultQual sorts before the name of
// @SubQual, so if the two defaults were ordered by name rather than by origin, then
// @ElementDefaultQual would win.

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultQual;
import org.checkerframework.framework.testchecker.util.SubQual;

@DefaultQualifier(value = ElementDefaultQual.class, locations = TypeUseLocation.FIELD)
public class ElementDefaultPrecedence {

  Object field;

  void useField() {
    @SubQual Object ok = field;
    // :: error: (assignment)
    @ElementDefaultQual Object bad = field;
  }
}
