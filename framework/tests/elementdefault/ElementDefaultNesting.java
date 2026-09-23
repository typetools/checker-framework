// Tests that a @DefaultQualifier annotation on a nested element takes precedence over a
// @DefaultQualifier annotation on an enclosing element, as the manual's rule 6 (the innermost
// user-written @DefaultQualifier) requires.
//
// The name of @ElementDefaultQual sorts before the name of @SubQual, so if the two defaults were
// ordered by name rather than by scope, then @ElementDefaultQual would win in class Inner too.

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultQual;
import org.checkerframework.framework.testchecker.util.SubQual;

@DefaultQualifier(value = ElementDefaultQual.class, locations = TypeUseLocation.FIELD)
public class ElementDefaultNesting {

  Object outerField;

  void useOuterField() {
    @ElementDefaultQual Object ok = outerField;
    // :: error: (assignment)
    @SubQual Object bad = outerField;
  }

  @DefaultQualifier(value = SubQual.class, locations = TypeUseLocation.FIELD)
  static class Inner {

    Object innerField;

    void useInnerField() {
      @SubQual Object ok = innerField;
      // :: error: (assignment)
      @ElementDefaultQual Object bad = innerField;
    }
  }
}

// Tests that a nested @DefaultQualifier for ALL takes precedence over an enclosing
// @DefaultQualifier for the more specific location RETURN.  If the defaults were ordered by
// location rather than by scope, then the return type of returnsSub would be @ElementDefaultQual.
@DefaultQualifier(value = ElementDefaultQual.class, locations = TypeUseLocation.RETURN)
class ElementDefaultNestingAll {

  static @SubQual Object sub;

  @DefaultQualifier(SubQual.class)
  static Object returnsSub() {
    return sub;
  }

  static void useReturn() {
    @SubQual Object ok = returnsSub();
    // :: error: (assignment)
    @ElementDefaultQual Object bad = returnsSub();
  }
}
