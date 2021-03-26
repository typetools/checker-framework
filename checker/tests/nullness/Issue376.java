// Test case for Issue 376:
// https://github.com/typetools/checker-framework/issues/376

public class Issue376 {

  interface I {}

  <Q extends Enum<Q> & I> void m(Class<Q> clazz, String name) {
    I i = Enum.valueOf(clazz, name);
  }
}
