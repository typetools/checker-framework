import checkers.nullness.quals.*;
import java.io.*;
import java.util.*;

class Cell<T extends @Nullable Object> {
  void add(T arg) { }
}

class GenericArgs2 {
  static <F extends Object> void test1(Cell<F> collection) {
    //:: error: (argument.type.incompatible)
    collection.add(null); // should fail
  }
  static <F extends @Nullable Object> void test2(Cell<F> collection) {
    //:: error: (argument.type.incompatible)
    collection.add(null); // should fail
  }
  static void test3(Cell<@Nullable Object> collection) {
    collection.add(null); // valid
  }
  // No "<F super Object>" version of the above, as that is illegal in Java.

  static class InvariantFilter { }
  static class Invariant { }

  HashMap<Class<? extends InvariantFilter>,Map<Class<? extends Invariant>,Integer>> filter_map1;
  MyMap<@Nullable Class<? extends InvariantFilter>,Map<Class<? extends Invariant>,Integer>> filter_map2;

  public GenericArgs2(HashMap<Class<? extends InvariantFilter>,Map<Class<? extends Invariant>,Integer>> filter_map1,
                      MyMap<@Nullable Class<? extends InvariantFilter>,Map<Class<? extends Invariant>,Integer>> filter_map2) {
    this.filter_map1 = filter_map1;
    this.filter_map2 = filter_map2;
  }
  
  class MyMap<K extends @Nullable Object, V extends @Nullable Object> {}
}
