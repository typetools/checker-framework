// Type argument inference asks the Value Checker for a dummy assignment context (see
// ValueAnnotatedTypeFactory.getDummyAssignedTo) when an invocation has no assignment context.  The
// dummy is javac's type for the invocation, which javac may have left unsubstituted when the result
// of the invocation is unused.  Inference must not use such a type as a target type, but it must
// still use a type that mentions only type variables that are in scope at the invocation.

import java.util.ArrayList;
import java.util.List;

public class UnusedInvocationResult {

  static <T> List<T> makeList(T t) {
    return new ArrayList<>();
  }

  static <T> T identity(T t) {
    return t;
  }

  static class Box<T> {
    Box(T t) {}

    <U> U unbox(U u) {
      return u;
    }
  }

  static class GenericConstructor {
    <T> GenericConstructor(T t) {}
  }

  // A call whose result is unused and whose type mentions the invoked method's type variable.
  void unusedResult(int[] array) {
    makeList(array);
    identity(array);
  }

  // A recursive call whose result is unused.  Javac's type for the call is the enclosing method's
  // `T`, which is the result of substituting `T` for `T`, not a missing substitution.
  static <T> T recursive(T t) {
    recursive(t);
    return t;
  }

  // The same, for a class's type variable rather than a method's.
  static class Recursive<T> {
    T field;

    void m(T t) {
      new Box<>(t);
      identity(t);
    }
  }

  // `Box` is generic but its constructor is not, so the type variable of an unsubstituted
  // `Box<T>` is declared by the class rather than by the constructor.
  void unusedNewClass(int[] array) {
    new Box<>(array);
    new GenericConstructor(array);
    new Box<>(array).unbox(array);
  }
}
