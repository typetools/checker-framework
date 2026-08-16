// Type argument inference asks the Value Checker for a dummy assignment context (see
// ValueAnnotatedTypeFactory.getDummyAssignedTo) when an invocation has no assignment context.  The
// dummy is javac's type for the invocation, which javac may have left unsubstituted.  Inference
// must not use such a type as a target type, but it must still use a type that mentions only type
// variables that are in scope at the invocation.
//
// Without InferenceFactory.mentionsUnsubstitutedTypeVariable, the call to `unsatisfiableCycle`
// below is issued a spurious "type.arguments.not.inferred" error.  The other invocations whose type
// javac leaves unsubstituted do not currently produce a diagnostic either way; they are here so
// that a change to which invocations the check applies to is a change to the test's behavior.

import java.util.ArrayList;
import java.util.List;

public class UnsubstitutedInvocationType {

  // Invocations whose type javac leaves unsubstituted.

  // Nothing constrains the type arguments but the type parameters' own bounds, so javac leaves the
  // type of a call as `T`.  Using `T` as the target type would require the inference variable that
  // stands for `T` to be a subtype of `T` itself.
  static <T extends S, S extends Comparable<T>> T unsatisfiableCycle() {
    throw new AssertionError();
  }

  static <T extends Comparable<T>> T fBounded() {
    throw new AssertionError();
  }

  void unsubstitutedMethodResult() {
    unsatisfiableCycle();
    fBounded();
  }

  // Javac infers `Builder`'s first type argument from the constructor argument but leaves the
  // second one as `B`.  The result of this invocation is used, as a receiver, so an unsubstituted
  // type is not peculiar to invocations whose result is unused.
  void unsubstitutedConstructorResult(int[] array) {
    new Builder<>(array).self();
  }

  static class Builder<S, B extends Builder<S, B>> {
    Builder(S s) {}

    @SuppressWarnings("unchecked")
    B self() {
      return (B) this;
    }
  }

  // `CycleBox` is generic but its constructor is not, so the type variables of an unsubstituted
  // `CycleBox<T, S>` are declared by the class rather than by the constructor.
  void unsubstitutedClassTypeArguments() {
    new CycleBox<>();
  }

  static class CycleBox<T extends S, S extends Comparable<T>> {
    CycleBox() {}
  }

  // Invocations whose type javac fully substitutes, so it is usable as a target type.

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

  void unusedResult(int[] array) {
    makeList(array);
    identity(array);
    new Box<>(array);
    new GenericConstructor(array);
    new Box<>(array).unbox(array);
  }

  // A recursive call whose result is unused.  Javac's type for the call is the enclosing method's
  // `T`, which is the result of substituting `T` for `T`, not a missing substitution.
  static <T> T recursive(T t) {
    recursive(t);
    return t;
  }

  // The same, for a class's type variable rather than a method's.
  static class Recursive<T> {
    void m(T t) {
      new Box<>(t);
      identity(t);
    }
  }
}
