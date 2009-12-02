import checkers.nullness.quals.*;
import java.io.*;
import java.util.*;

class Cell<T extends @Nullable Object> {
  void add(T arg) { }
}

class GenericArgs2 {
  static <F extends Object> void test1(Cell<F> collection) {
    collection.add(null); // should fail
  }
  static <F extends @Nullable Object> void test2(Cell<F> collection) {
    collection.add(null); // should fail
  }
  // No "<F super Object>" version of the above, as that is illegal in Java.
}
