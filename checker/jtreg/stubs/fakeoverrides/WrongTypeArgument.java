package fakeoverrides;

import java.util.List;

/*
 * @test
 * @summary A stub method whose formal parameter type has the wrong type argument is not a fake
 * override, so it is reported as not found.
 *
 * WrongTypeArgument.goal contains the expected warning.
 *
 * @compile/ref=WrongTypeArgument.goal -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -Astubs=WrongTypeArgument.astub -AstubWarnIfNotFound -Anomsgtext WrongTypeArgument.java
 */
public class WrongTypeArgument {}

class WrongEnclosing<T> {
  class WrongNested<U> {}
}

interface WrongTypeArgumentSuper {
  default int g(List<String> l) {
    return 0;
  }

  default int enclosing(WrongEnclosing<String>.WrongNested<Integer> e) {
    return 0;
  }
}

class WrongTypeArgumentSub implements WrongTypeArgumentSuper {}
