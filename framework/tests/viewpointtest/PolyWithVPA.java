import viewpointtest.quals.*;

public class PolyWithVPA {
  static class PolyClass {
    @ReceiverDependentQual
    Object foo(@PolyVP Object o) {
      return null;
    }
  }

  static void test1(@A PolyClass a, @B Object bObj) {
    @A Object aObj = a.foo(bObj);
  }

  // only poly annos in decl are resolved
  static void test2(@PolyVP PolyClass poly, @B Object bObj) {
    @PolyVP Object polyObj = poly.foo(bObj);
    // :: error: (assignment)
    @B Object anotherBObj = poly.foo(bObj);
  }
}
