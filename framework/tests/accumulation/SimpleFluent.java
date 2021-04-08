// A simple test that the fluent API logic in the Accumulation Checker works.

import org.checkerframework.common.returnsreceiver.qual.*;
import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

/* Simple inference of a fluent builder. */
public class SimpleFluent {
  SimpleFluent build(@TestAccumulation({"a", "b"}) SimpleFluent this) {
    return this;
  }

  @This SimpleFluent build2(@TestAccumulation({"a", "b"}) SimpleFluent this) {
    return this;
  }

  @This SimpleFluent a() {
    return this;
  }

  @This SimpleFluent b() {
    return this;
  }

  // intentionally does not have an @This annotation
  SimpleFluent c() {
    return this;
  }

  static void doStuffCorrect(@TestAccumulation({"a", "b"}) SimpleFluent s) {
    s.a().b().build();
  }

  static void doStuffWrong(@TestAccumulation({"a"}) SimpleFluent s) {
    s.a()
        // :: error: method.invocation.invalid
        .build();
  }

  static void noReturnsReceiverAnno(@TestAccumulation({"a", "b"}) SimpleFluent s) {
    s.a()
        .b()
        .c()
        // :: error: method.invocation.invalid
        .build();
  }

  static void mixFluentAndNonFluent(SimpleFluent s1) {
    s1.a().b();
    s1.build();
  }

  static void mixFluentAndNonFluentWrong(SimpleFluent s) {
    s.a(); // .b()
    // :: error: method.invocation.invalid
    s.build();
  }

  static void fluentLoop(SimpleFluent t) {
    SimpleFluent s = t.a();
    int i = 10;
    while (i > 0) {
      // :: error: method.invocation.invalid
      s.b().build();
      i--;
      s = new SimpleFluent();
    }
  }

  static void m1(SimpleFluent s) {
    s.c().a().b().build();
  }

  static void m2(SimpleFluent s) {
    s.c().a().b();
    // :: error: method.invocation.invalid
    s.c().build();
  }

  static void m3(SimpleFluent s) {
    s.c().a().b().build();
    // :: error: method.invocation.invalid
    s.c().a().build();
  }

  static void m4(SimpleFluent s) {
    s.c().a().b().build2().build();
  }

  static void m5(SimpleFluent s) {
    s.a().c();
    s.b().build();
  }

  static void m6(SimpleFluent s) {
    // :: error: method.invocation.invalid
    s.a().c().b().build();
  }

  static void m7(SimpleFluent s) {
    // :: error: method.invocation.invalid
    s.a().b().build().c().b().build();
  }

  static void m8(SimpleFluent s) {
    // :: error: method.invocation.invalid
    s.a().build().c().a().b().build();
    // :: error: method.invocation.invalid
    s.build();
  }

  static void m9() {
    new SimpleFluent().a().b().build();
    // :: error: method.invocation.invalid
    new SimpleFluent().a().build();
  }
}
