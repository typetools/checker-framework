import org.checkerframework.framework.testchecker.h1h2checker.quals.*;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Invalid;

public class TypeRefinement {
  // :: warning: (cast.unsafe.constructor.invocation)
  @H1Top Object o = new @H1S1 Object();
  // :: error: (h1h2checker.h1invalid.forbidden) :: warning: (cast.unsafe.constructor.invocation)
  @H1Top Object o2 = new @H1Invalid Object();
  // :: error: (h1h2checker.h1invalid.forbidden)
  @H1Top Object o3 = getH1Invalid();

  // :: error: (h1h2checker.h1invalid.forbidden)
  @H1Invalid Object getH1Invalid() {
    // :: error: (h1h2checker.h1invalid.forbidden) :: warning:
    // (cast.unsafe.constructor.invocation)
    return new @H1Invalid Object();
  }
}
