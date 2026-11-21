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
      // `k` is a key for the record's map, not for this.m.  Its type is @KeyFor("o.m"), which
      // cannot be written in source code because `o` has type Object.  The `adapted` and
      // `adaptedExplicitType` methods test the qualifier when the matched value is a KeyedMap.
      // :: error: [unboxing.of.nullable]
      int i = m.get(k);
    }
  }

  static Map<String, Integer> staticMap = Map.of();

  record StaticKey(@KeyFor("InstanceOfDeconstructionDependentType.staticMap") String k) {}

  StaticKey getStaticKey() {
    throw new Error();
  }

  void receiverIndependent() {
    // getStaticKey() is not deterministic, but the qualifier does not depend on the record.
    if (getStaticKey() instanceof StaticKey(var k)) {
      int i = staticMap.get(k);
    }
  }

  void adapted(KeyedMap km) {
    if (km instanceof KeyedMap(var mm, var k)) {
      int i = km.m.get(k);
    }
  }

  void reassigned(KeyedMap km, KeyedMap other) {
    if (km instanceof KeyedMap(var mm, var k)) {
      km = other;
      // `km` is reassigned, so `k` is not a key for km.m.
      // :: error: [unboxing.of.nullable]
      int i = km.m.get(k);
    }
  }

  void reassignedReceiverIndependent(Object o, Object other) {
    if (o instanceof StaticKey(var k)) {
      o = other;
      int i = staticMap.get(k);
    }
  }

  void reassignedNested(Outer o, Outer other) {
    if (o instanceof Outer(KeyedMap(var mm, String k))) {
      o = other;
      // `o` is reassigned, so `k` is not a key for o.km().m.
      // :: error: [unboxing.of.nullable]
      int i = o.km().m.get(k);
    }
  }

  void adaptedExplicitType(KeyedMap km) {
    if (km instanceof KeyedMap(var mm, String k)) {
      @KeyFor("km.m") String k2 = k;
      // :: error: [assignment]
      @KeyFor("m") String k3 = k;
    }
  }

  record Outer(KeyedMap km) {}

  Outer getOuter() {
    throw new Error();
  }

  void nestedDeterministic(Outer o) {
    if (o instanceof Outer(KeyedMap(var mm, String k))) {
      @KeyFor("o.km().m") String k2 = k;
    }
  }

  void nestedNondeterministic() {
    // getOuter() is not deterministic, so `k` is not a key for getOuter().km().m.
    if (getOuter() instanceof Outer(KeyedMap(var mm, String k))) {
      // :: error: [assignment]
      @KeyFor("getOuter().km().m") String k2 = k;
    }
  }

  void nestedConditional(boolean flag, Outer o1, Outer o2) {
    // The matched expression is not deterministic, so `k` is not a key for a map of either record.
    if ((flag ? o1 : o2) instanceof Outer(KeyedMap(var mm, String k))) {
      // :: error: [assignment]
      @KeyFor("o1.km().m") String k2 = k;
      // :: error: [unboxing.of.nullable]
      int i = o1.km().m().get(k);
    }
  }

  void adaptedSwitch(KeyedMap km) {
    switch (km) {
      case KeyedMap(var mm, String k) -> {
        @KeyFor("km.m") String k2 = k;
        // :: error: [assignment]
        @KeyFor("m") String k3 = k;
      }
    }
  }
}
