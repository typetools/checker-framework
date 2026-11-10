// Tests that a default registered through QualifierDefaults.addElementDefault takes precedence
// over a @DefaultQualifier annotation at the same location on the same element, but not over a
// @DefaultQualifier annotation on a nested element.
//
// ElementDefaultAnnotatedTypeFactory registers @SubQual for FIELD on the class below, which also
// has a @DefaultQualifier for FIELD.  The name of @ElementDefaultQual sorts before the name of
// @SubQual, so if the two defaults were ordered by name rather than by origin, then
// @ElementDefaultQual would win.

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultQual;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

@DefaultQualifier(value = ElementDefaultQual.class, locations = TypeUseLocation.FIELD)
public class ElementDefaultPrecedence {

  Object field;

  void useField() {
    @SubQual Object ok = field;
    // :: error: (assignment)
    @ElementDefaultQual Object bad = field;
  }

  // The field is @SuperQual, from the @DefaultQualifier on the nested class, which takes
  // precedence over both the registered default and the @DefaultQualifier on the enclosing class.
  @DefaultQualifier(value = SuperQual.class, locations = TypeUseLocation.FIELD)
  static class Nested {

    Object nestedField;

    void useNestedField() {
      @SuperQual Object ok = nestedField;
      // :: error: (assignment)
      @SubQual Object notRegistered = nestedField;
      // :: error: (assignment)
      @ElementDefaultQual Object notEnclosing = nestedField;
    }
  }
}
