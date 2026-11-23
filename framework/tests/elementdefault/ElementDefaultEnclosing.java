// Tests that a default registered through QualifierDefaults.addElementDefault composes with the
// @DefaultQualifier annotations on an enclosing scope, and that a registered alias is replaced by
// its canonical qualifier.
//
// ElementDefaultAnnotatedTypeFactory computes the type of Inner.takesSub(), and then registers
// @Critical, an alias for @SubQual, for RETURN on the class Inner below.  The registration must not
// affect the enclosing class, even though the defaults of Inner were computed from those of the
// enclosing class before the registration.

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

@DefaultQualifier(value = SubQual.class, locations = TypeUseLocation.PARAMETER)
public class ElementDefaultEnclosing {

  @SuperQual Object superField;

  // The return type is @SuperQual, from @DefaultQualifierInHierarchy on SuperQual.  The default
  // registered for Inner does not apply here.
  Object returnsSuper() {
    return superField;
  }

  void useOuterReturn() {
    // :: error: (assignment)
    @SubQual Object bad = returnsSuper();
  }

  static class Inner {

    @SubQual Object subField;

    // The return type is @SubQual, from the default that addElementDefault registered for Inner.
    Object returnsSub() {
      return subField;
    }

    void useReturn() {
      @SubQual Object ok = returnsSub();
    }

    // The parameter is @SubQual, from the @DefaultQualifier on the enclosing class.
    void takesSub(Object param) {}

    void useParam(@SubQual Object sub, @SuperQual Object sup) {
      takesSub(sub);
      // :: error: (argument)
      takesSub(sup);
    }
  }
}
