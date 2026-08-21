// Exercises the varargs expansion in AbstractExecutableType.getParameterTypes, which replaces the
// vararg formal parameter by one copy of its component type per actual argument.

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused") // the locals exist only to supply a target type
public class VarargsExpansion {

  void zeroVarargs() {
    List<String> l = listOf();
  }

  void oneVararg() {
    List<String> l = listOf("a");
  }

  void manyVarargs() {
    List<String> l = listOf("a", "b", "c");
  }

  void arrayArgument() {
    // Not a varargs invocation: the array is passed directly as the vararg formal parameter.
    List<String> l = listOf(new String[] {"a", "b"});
  }

  void nonVarargFormalBeforeVararg() {
    List<String> l = prepend("a", "b", "c");
  }

  void nestedGenericVarargs() {
    List<List<String>> l = listOf(listOf("a"), listOf("b"));
  }

  void lambdaVarargs() {
    List<Supplier<String>> l = listOf(() -> "a", () -> "b");
  }

  void varargsConstructor(String s, String t) {
    Box<String> b = new Box<>(s, t);
  }

  void varargsMethodReference() {
    Function<String, List<String>> f = VarargsExpansion::listOf;
  }

  void unboundVarargsMethodReference() {
    // The function type's first parameter is the receiver, so the compile-time declaration has one
    // more parameter than the method itself declares.
    BiFunction<VarargsExpansion, String, List<String>> f = VarargsExpansion::instanceListOf;
  }

  void varargsConstructorReference() {
    Function<String, Box<String>> f = Box::new;
  }

  @SafeVarargs
  static <T> List<T> listOf(T... ts) {
    return new ArrayList<>();
  }

  @SafeVarargs
  static <T> List<T> prepend(T first, T... rest) {
    return new ArrayList<>();
  }

  @SafeVarargs
  private final <T> List<T> instanceListOf(T... ts) {
    return new ArrayList<>();
  }

  static class Box<T> {
    @SafeVarargs
    Box(T... ts) {}
  }
}
