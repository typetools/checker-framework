import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;

// H1H2AnnotatedTypeFactory overrides getTypeDeclarationBounds so that the qualifier upper bound of
// H1S1Bounded is @H1S1 in the H1 hierarchy, though no annotation on the class says so.
public class TypeDeclarationBoundsOverride {

  void uses(@H1S1 H1S1Bounded p) {}

  // :: error: [annotations.on.use]
  void invalid(@H1Top H1S1Bounded p) {}

  void cast(@H1Top Object o) {
    // The type of the cast is limited to the bound @H1S1.
    @H1S1 H1S1Bounded b = (H1S1Bounded) o;
  }

  void arrayInitializer(@H1Top Object o) {
    // The type of the array element is limited to the bound @H1S1.
    @H1S1 Object[] a = new H1S1Bounded[] {(H1S1Bounded) o};
  }
}

// The default qualifier of the class's own type is @H1Top, which is not below its bound.
// :: error: [annotations.on.use]
class H1S1Bounded {}
