// Test that an overriding method's type parameter bounds contain the overridden method's type
// parameter bounds.  A client may call the overridden method with any type argument that is
// within the overridden method's bounds, so the overriding method must permit all of them.

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class OverrideTypeParamBound {

  // The overriding method raises the lower bound, so it forbids the type argument
  // @NonNull String that a client may supply.
  static class LowerBoundRaisedSuper {
    <T extends @Nullable Object> T pick(T p) {
      return p;
    }
  }

  static class LowerBoundRaisedSub extends LowerBoundRaisedSuper {
    @Override
    // :: error: [override.type.parameter.lower]
    <@Nullable T extends @Nullable Object> T pick(T p) {
      T t = null;
      return t;
    }
  }

  // Without the error above, this method would dereference null.
  void clientOfLowerBoundRaised(LowerBoundRaisedSuper s) {
    @NonNull String r = s.<@NonNull String>pick("x");
    r.length();
  }

  // The overriding method lowers the upper bound, so it forbids the type argument
  // @Nullable String that a client may supply.
  static class UpperBoundLoweredSuper {
    <T extends @Nullable Object> T pick(T p) {
      return p;
    }
  }

  static class UpperBoundLoweredSub extends UpperBoundLoweredSuper {
    @Override
    // :: error: [override.type.parameter.upper]
    <T extends @NonNull Object> T pick(T p) {
      p.hashCode();
      return p;
    }
  }

  // The type parameter appears only in the return type, where neither the parameter check nor
  // the return check constrains its bounds.
  static class ReturnOnlySuper {
    <T extends @Nullable Object> T make() {
      throw new Error("not implemented");
    }
  }

  static class ReturnOnlySub extends ReturnOnlySuper {
    @Override
    // :: error: [override.type.parameter.lower]
    <@Nullable T extends @Nullable Object> T make() {
      T t = null;
      return t;
    }
  }

  // The type parameter appears only within a parameter's type argument.
  static class NestedOnlySuper {
    <T extends @Nullable Object> void f(List<T> x) {}
  }

  static class NestedOnlySub extends NestedOnlySuper {
    @Override
    // :: error: [override.type.parameter.lower]
    <@Nullable T extends @Nullable Object> void f(List<T> x) {
      x.set(0, null);
    }
  }

  // The type parameter does not appear in the signature at all.
  static class UnusedTypeParamSuper {
    <T extends @Nullable Object> void g() {}
  }

  static class UnusedTypeParamSub extends UnusedTypeParamSuper {
    @Override
    // :: error: [override.type.parameter.upper]
    <T extends @NonNull Object> void g() {}
  }

  // Widening the bounds is legal:  the overriding method permits every type argument that the
  // overridden method permits.
  static class UpperBoundRaisedSuper {
    <T extends @NonNull Object> T pick(T p) {
      return p;
    }
  }

  static class UpperBoundRaisedSub extends UpperBoundRaisedSuper {
    @Override
    <T extends @Nullable Object> T pick(T p) {
      return p;
    }
  }

  // Renaming the type parameter has no effect.
  static class RenamedSuper {
    <T extends @Nullable Object> T pick(T p) {
      return p;
    }
  }

  static class RenamedSub extends RenamedSuper {
    @Override
    <S extends @Nullable Object> S pick(S p) {
      return p;
    }
  }

  // A bound that mentions the method's own type parameters.
  static class RecursiveBoundSuper {
    <T extends Comparable<T>, U extends T> T max(T p1, U p2) {
      return p1;
    }
  }

  static class RecursiveBoundSub extends RecursiveBoundSuper {
    @Override
    <T extends Comparable<T>, U extends T> T max(T p1, U p2) {
      return p1;
    }
  }

  // A primary annotation on a use of the type parameter is checked covariantly in the return
  // type and contravariantly in a parameter type.

  static class PrimaryAnnotationSuper {
    <T extends @Nullable Object> T pick(T p) {
      return p;
    }

    <T extends @Nullable Object> void f(@NonNull T p) {}
  }

  static class PrimaryAnnotationSub extends PrimaryAnnotationSuper {
    // Strengthening the return type is legal.
    @Override
    <T extends @Nullable Object> @NonNull T pick(T p) {
      throw new Error("not implemented");
    }

    // Weakening a parameter type is legal.
    @Override
    <T extends @Nullable Object> void f(T p) {}
  }

  static class BadPrimaryAnnotationSuper {
    <T extends @Nullable Object> T pick(T p) {
      return p;
    }

    <T extends @Nullable Object> void f(T p) {}
  }

  static class BadPrimaryAnnotationSub extends BadPrimaryAnnotationSuper {
    // Weakening the return type is illegal:  the client's type argument may be @NonNull.
    @Override
    // :: error: [override.return]
    <T extends @Nullable Object> @Nullable T pick(T p) {
      return null;
    }

    // Strengthening a parameter type is illegal:  the client's type argument may be @Nullable.
    @Override
    // :: error: [override.param]
    <T extends @Nullable Object> void f(@NonNull T p) {
      p.hashCode();
    }
  }

  // A method reference is checked as an override of the functional interface's method, but the
  // type parameters of the method reference's compile-time declaration do not correspond to those
  // of the functional interface's method.  Instead, the compile-time declaration's type arguments,
  // which are inferred, are checked against its bounds.

  interface ListConsumer {
    <T extends @Nullable Object> void use(List<T> list);
  }

  static <@Nullable U extends @Nullable Object> void addNull(List<U> list) {
    list.add(null);
  }

  static <U extends @Nullable Object> void ignore(List<U> list) {}

  void methodReferences() {
    // :: error: [type.argument]
    ListConsumer bad = OverrideTypeParamBound::addNull;
    ListConsumer good = OverrideTypeParamBound::ignore;
  }

  static class Box<E> {
    <U extends @NonNull Object> U id(U u) {
      return u;
    }
  }

  // The ReferenceType is raw, so the class's type arguments are inferred, but the method's type
  // arguments are explicit.
  @SuppressWarnings({"rawtypes", "unchecked"})
  void rawReferenceTypeWithExplicitTypeArguments() {
    BiFunction<Box, String, Object> good = Box::<String>id;
    // :: error: [type.argument]
    BiFunction<Box, @Nullable String, @Nullable Object> bad = Box::<@Nullable String>id;
  }

  // Without the error above, this method would dereference null.
  void clientOfMethodReference(ListConsumer c) {
    List<@NonNull String> list = new ArrayList<>();
    c.use(list);
    list.get(0).length();
  }
}
