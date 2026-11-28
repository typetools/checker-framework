import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S1;

// The qualifier upper bound of a type has no qualifier in the H2 hierarchy.  The type of a cast
// keeps the H2 qualifier of the casted expression.
public class CastWithoutBound {

  void cast(@H1S1 @H2S1 Object o) {
    @H1S1 @H2S1 String s = (String) o;
  }

  void arrayInitializer(@H1S1 @H2S1 Object o) {
    Object @H1S1 [] a = new Object[] {o};
  }
}
