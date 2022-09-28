// @above-java17-jdk-skip-test TODO: reinstate on JDK 18, false positives may be due to issue #979

import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.framework.testchecker.lib.UncheckedByteCode;

public class UpperBoundsInByteCode {
  UncheckedByteCode<@Fenum("Foo") String> foo;
  UncheckedByteCode<@Fenum("Bar") Object> bar;

  void typeVarWithNonObjectUpperBound(@Fenum("A") int a) {
    // :: error: (type.argument)
    UncheckedByteCode.methodWithTypeVarBoundedByNumber(a);
    UncheckedByteCode.methodWithTypeVarBoundedByNumber(1);
  }

  void wildcardsInByteCode() {
    UncheckedByteCode.unboundedWildcardParam(foo);
    UncheckedByteCode.lowerboundedWildcardParam(bar);
    // :: error: (argument)
    UncheckedByteCode.upperboundedWildcardParam(foo);
  }

  // :: error: (type.argument)
  SourceCode<@Fenum("Foo") String> foo2;

  class SourceCode<T extends Object> {}
}
