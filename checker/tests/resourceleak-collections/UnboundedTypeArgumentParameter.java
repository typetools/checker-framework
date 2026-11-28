// Passing a resource collection to a method whose parameter has an unbounded wildcard or type
// variable as its type argument, such as `List<?>` or `<T> List<T>`.  Such a method cannot add a
// resource to the collection, so passing a resource collection to it is safe.  An
// `@OwningCollection` parameter takes ownership of the elements, so its element type must say which
// methods to call on them.

import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class UnboundedTypeArgumentParameter {

  static int countWildcard(List<?> list) {
    return list.size();
  }

  static <T> int countTypeVariable(List<T> list) {
    return list.size();
  }

  static <T> int countUpperBoundedWildcard(List<? extends T> list) {
    return list.size();
  }

  static <T> int countLowerBoundedWildcard(List<? super T> list) {
    return list.size();
  }

  // The bound of `U` is the type variable `T`, whose bound is `Object`.
  static <T, U extends T> int countTypeVariableChain(List<U> list) {
    return list.size();
  }

  static <T, U extends T, V extends U> int countLongTypeVariableChain(List<V> list) {
    return list.size();
  }

  static <T, U extends T> int countUpperBoundedWildcardChain(List<? extends U> list) {
    return list.size();
  }

  // The bound of `T` is written, so the chain from `U` is not widened.
  static <T extends @MustCall({}) Object, U extends T> int countWrittenBoundChain(List<U> list) {
    return list.size();
  }

  static void takeWildcard(@OwningCollection List<?> list) {}

  static <T> void takeTypeVariable(@OwningCollection List<T> list) {}

  void passToWildcard(@OwningCollection List<Resource> resources) {
    countWildcard(resources);
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }

  void passToTypeVariable(@OwningCollection List<Resource> resources) {
    countTypeVariable(resources);
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }

  void passToBoundedWildcards(@OwningCollection List<Resource> resources) {
    countUpperBoundedWildcard(resources);
    countLowerBoundedWildcard(resources);
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }

  static class Counter {
    Counter(List<?> list) {}
  }

  void passToConstructorWildcard(@OwningCollection List<Resource> resources) {
    new Counter(resources);
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }

  void passToTypeVariableChains(@OwningCollection List<Resource> resources) {
    countTypeVariableChain(resources);
    countLongTypeVariableChain(resources);
    countUpperBoundedWildcardChain(resources);
    // :: error: type.arguments.not.inferred
    countWrittenBoundChain(resources);
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }

  void passToOwningWildcard(@OwningCollection List<Resource> resources) {
    // :: error: argument
    takeWildcard(resources);
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }

  void passToOwningTypeVariable(@OwningCollection List<Resource> resources) {
    // :: error: type.arguments.not.inferred
    takeTypeVariable(resources);
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }
}
