// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that dependent-type qualifiers on a record component are viewpoint-adapted to the value that
// a deconstruction pattern matches.

import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;

public class InstanceOfDeconstructionDependentType {

  Map<String, Integer> m = Map.of();

  record KeyedMap(Map<String, Integer> m, @KeyFor("m") String k) {}

  void dependentType(Object o) {
    if (o instanceof KeyedMap(var mm, var k)) {
      // `k` is a key for the record's map, not for this.m.
      // :: error: [unboxing.of.nullable]
      int i = m.get(k);
    }
  }
}
