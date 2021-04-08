import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

/* Simple inference of a fluent builder */
public class SimpleFluentInference {
  SimpleFluentInference build(@CalledMethods({"a", "b"}) SimpleFluentInference this) {
    return this;
  }

  SimpleFluentInference weakbuild(@CalledMethods({"a"}) SimpleFluentInference this) {
    return this;
  }

  @This SimpleFluentInference a() {
    return this;
  }

  @This SimpleFluentInference b() {
    return this;
  }

  // intentionally does not have an @This annotation
  SimpleFluentInference c() {
    return new SimpleFluentInference();
  }

  static void doStuffCorrect() {
    SimpleFluentInference s = new SimpleFluentInference().a().b().build();
  }

  static void doStuffWrong() {
    SimpleFluentInference s =
        new SimpleFluentInference()
            .a()
            // :: error: finalizer.invocation.invalid
            .build();
  }

  static void doStuffRightWeak() {
    SimpleFluentInference s = new SimpleFluentInference().a().weakbuild();
  }

  static void noReturnsReceiverAnno() {
    SimpleFluentInference s =
        new SimpleFluentInference()
            .a()
            .b()
            .c()
            // :: error: finalizer.invocation.invalid
            .build();
  }

  static void fluentLoop() {
    SimpleFluentInference s = new SimpleFluentInference().a();
    int i = 10;
    while (i > 0) {
      // :: error: finalizer.invocation.invalid
      s.b().build();
      i--;
      s = new SimpleFluentInference();
    }
  }
}
