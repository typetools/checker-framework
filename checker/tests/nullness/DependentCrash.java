import org.checkerframework.checker.nullness.qual.KeyFor;

import java.util.Map;

// Test for Issue 259
// https://code.google.com/p/checker-framework/issues/detail?id=259
abstract class Precondition {

  abstract <T> T checkNotNull(T reference);

  class Foo {
    int x;
  }

  int method(Map<Object, Foo> map, Object key) {
    //:: error: (dereference.of.nullable)
    return checkNotNull(map.get(key)).x;
  }

  int method2(Map<Object, Foo> map, @KeyFor("#1") Object key) {
    return (map.get(key)).x;
  }
}
