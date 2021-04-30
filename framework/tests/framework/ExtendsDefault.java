import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.*;

public class ExtendsDefault {

  @DefaultQualifier(
      value = Odd.class,
      locations = {TypeUseLocation.UPPER_BOUND})
  class MyOddDefault<T> {}

  class MyNonOddDefault<T> {}

  void testNonOdd() {
    // :: error: (type.argument)
    MyOddDefault<String> s1;
    MyNonOddDefault<String> s2;
  }

  void testOdd() {
    MyOddDefault<@Odd String> s1;
    MyNonOddDefault<@Odd String> s2;
  }
}
