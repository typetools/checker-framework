// @below-java17-jdk-skip-test
// Test case for https://github.com/typetools/checker-framework/issues/5240

import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;

public class InstanceOfPatternVariable {

  public void doSomething(final Object x) {
    if (x instanceof Map<?, ?> m) {
      // final var ct = (ClassOrInterfaceType) type;

      @KeyFor("m") Object y = m.keySet().iterator().next();
    }
  }
}
