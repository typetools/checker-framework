// Tests a checker whose applier overrides shouldBeAnnotated so that no FIELD default applies.
// The override reads the location of the default being applied.
//
// If the override saw the wrong location, then the @DefaultQualifier below would make the type of
// the field @SubQual, and the assignment below would type-check.  Because the FIELD default does
// not apply, the type of the field is @SuperQual, from @DefaultQualifierInHierarchy.

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.SubQual;

@DefaultQualifier(value = SubQual.class, locations = TypeUseLocation.FIELD)
public class CustomShouldBeAnnotated {

  static Object superField;

  static void useField() {
    // :: error: (assignment)
    @SubQual Object bad = superField;
  }
}
