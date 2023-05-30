// This test checks that the leastUpperBound and mostSpecific methods in AccumulationValue.java
// behave as expected, in the context of resource leak checking (which is their first client).

import org.checkerframework.checker.mustcall.qual.*;

public class AccumulationValueTest {

  @InheritableMustCall({"a", "b"})
  class MCAB {
    void a() {}

    void b() {}
  }

  <T extends MCAB> void simple1(@MustCall({"a", "b"}) T mcab) {
    // test that an accumulation value can accumulate more than one item
    mcab.a();
    mcab.b();
  }

  // :: error: required.method.not.called
  <T extends MCAB> void simple2(@MustCall({"a", "b"}) T mcab) {
    // test that the RLC handles missing a in the must call type
    mcab.b();
  }

  // :: error: required.method.not.called
  <T extends MCAB> void simple3(@MustCall({"a", "b"}) T mcab, boolean b) {
    // tests lubbing two AccumulationValue at a join
    if (b) mcab.a();
    mcab.b();
  }

  // :: error: ? but there definitely should be one
  <T extends MCAB> void simple4(@MustCall({"a"}) T mcab) {
    // test that incompatible bounds leads to a warning
    mcab.a();
  }
}
