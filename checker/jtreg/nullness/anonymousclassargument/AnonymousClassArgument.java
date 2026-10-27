/*
 * @test
 * @summary Diagnostics about an argument to an anonymous class creation name the super constructor
 * and its parameter, not the synthesized anonymous constructor and its parameter.
 *
 * @compile/fail/ref=AnonymousClassArgument.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker AnonymousClassArgument.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AnonymousClassArgument {

  static class Foo {
    Foo(Object fooParam) {}

    <T extends @NonNull Object> Foo(T genericParam, int i) {}
  }

  class Inner {
    Inner(Object innerParam) {}
  }

  void test(@Nullable Object nble, AnonymousClassArgument outer) {
    new Foo(nble) {};
    new <@Nullable String>Foo("", 0) {};
    outer.new Inner(nble) {};
  }
}
