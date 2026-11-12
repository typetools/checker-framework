// Tests that a default registered through QualifierDefaults.addElementDefault composes with the
// @DefaultQualifier annotations on an enclosing scope, and that a registered alias is replaced by
// its canonical qualifier.
//
// ElementDefaultAnnotatedTypeFactory registers @Critical, an alias for @SubQual, for RETURN on the
// class Inner below.

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

@DefaultQualifier(value = SubQual.class, locations = TypeUseLocation.PARAMETER)
public class ElementDefaultEnclosing {

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
