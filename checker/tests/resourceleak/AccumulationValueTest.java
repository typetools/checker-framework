// This test checks that the leastUpperBound and mostSpecific methods in AccumulationValue.java
// behave as expected, in the context of resource leak checking (which is their first client).

import org.checkerframework.checker.mustcall.qual.*;

public class AccumulationValueTest {

  @InheritableMustCall({"a", "b"})
  class MCAB {
    void a() {}

    void b() {}

    void c() {}
  }

  <T extends MCAB> void simple1(@Owning @MustCall({"a", "b"}) T mcab) {
    // test that an accumulation value can accumulate more than one item
    mcab.a();
    mcab.b();
  }

  // :: error: required.method.not.called
  <T extends MCAB> void simple2(@Owning @MustCall({"a", "b"}) T mcab) {
    // test that the RLC handles missing call to a()
    mcab.b();
  }

  <T extends MCAB> void simple3(@Owning @MustCall({"a", "b"}) T mcab) {
    // test that an accumulation value can accumulate extra items without issue (this tests
    // mostSpecific)
    mcab.a();
    mcab.b();
    mcab.c();
  }

  // :: error: required.method.not.called
  <T extends MCAB> void lub1(@Owning @MustCall({"a", "b"}) T mcab, boolean b) {
    // tests lubbing two AccumulationValue at a join
    if (b) {
      mcab.a();
    }
    mcab.b();
  }

  <T extends MCAB> void lub2(@Owning @MustCall({"a", "b"}) T mcab, boolean b) {
    // tests lubbing two AccumulationValue at a join
    if (b) mcab.a();
    else mcab.a();
    mcab.b();
  }

  // :: error: required.method.not.called
  <T extends MCAB> void lub3(@Owning @MustCall({"a", "b"}) T mcab, boolean b) {
    // tests lubbing two AccumulationValue at a join if both are non-empty but non-intersecting
    if (b) {
      mcab.a();
    } else {
      mcab.b();
    }
  }

  // :: error: required.method.not.called
  <T extends MCAB> void lub4(@Owning @MustCall({"a", "b"}) T mcab, boolean b) {
    // tests lubbing two AccumulationValue at a join if both are non-empty but intersecting
    if (b) {
      mcab.a();
      mcab.c();
    } else {
      mcab.a();
      mcab.b();
    }
  }

  <T extends MCAB> void lub5(@Owning @MustCall({"a", "b"}) T mcab, boolean b) {
    // tests lubbing two AccumulationValue at a join if both are non-empty but intersecting
    if (b) {
      mcab.a();
      mcab.b();
      mcab.c();
    } else {
      mcab.a();
      mcab.b();
    }
  }

  // These two paired methods show what happens when the @MustCall type is "too small":
  // errors at call sites.
  <T extends MCAB> void wrongMCAnno(@Owning @MustCall({"a"}) T mcab) {
    mcab.a();
  }

  void wrongMCAnnoUse(@Owning MCAB mcab) {
    // :: error: argument
    wrongMCAnno(mcab);
  }
}
