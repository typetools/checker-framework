import viewpointtest.quals.*;

public class VPAExamples {

  static class RDContainer {
    @ReceiverDependentQual
    Object get() {
      return null;
    }

    void set(@ReceiverDependentQual Object o) {}

    @ReceiverDependentQual Object field;
  }

  void tests(@A RDContainer a, @B RDContainer b, @Top RDContainer top) {
    @A Object aObj = a.get();
    @B Object bObj = b.get();
    @Top Object tObj = top.get();
    // :: error: (assignment)
    bObj = a.get();
    // :: error: (assignment)
    aObj = top.get();
    // :: error: (assignment)
    bObj = top.get();

    a.set(aObj);
    // :: error: (argument)
    a.set(bObj);
    // :: error: (argument)
    b.set(aObj);
    b.set(bObj);
    top.set(aObj);
    top.set(bObj);
  }
}
