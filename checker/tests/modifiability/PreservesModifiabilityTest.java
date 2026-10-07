import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.PreservesModifiability;
import org.checkerframework.checker.modifiability.qual.Replaceable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;

class PreservesModifiabilityTest {

  @PreservesModifiability
  static <T> List<T> annotated(Collection<T> values) {
    return new ArrayList<>(values);
  }

  static <T> List<T> unannotated(Collection<T> values) {
    return new ArrayList<>(values);
  }

  // The annotation relates the result to the first argument, so it may not be written on a method
  // that returns void or that does not have exactly one formal parameter.
  // :: error: [preservesmodifiability.location]
  @PreservesModifiability
  static <T> void annotatedVoid(Collection<T> values) {}

  // :: error: [preservesmodifiability.location]
  @PreservesModifiability
  static <T> List<T> annotatedNoArguments() {
    return new ArrayList<>();
  }

  // :: error: [preservesmodifiability.location]
  @PreservesModifiability
  static <T> List<T> annotatedTwoArguments(Collection<T> values, Collection<T> other) {
    return new ArrayList<>(values);
  }

  // A varargs method has one formal parameter, but the first argument of a call to it is an
  // element of the varargs array rather than that parameter.
  // :: error: [preservesmodifiability.location]
  @PreservesModifiability
  @SafeVarargs
  static <T> List<T> annotatedVarargs(Collection<T>... values) {
    return new ArrayList<>(values[0]);
  }

  void preservesCapabilities(
      @Growable List<String> growable,
      @Shrinkable List<String> shrinkable,
      @Replaceable List<String> replaceable,
      @IteratorPolyMod List<String> iteratorPoly,
      @Modifiable List<String> modifiable) {
    @Growable List<String> g = annotated(growable);
    @Shrinkable List<String> s = annotated(shrinkable);
    @Replaceable List<String> r = annotated(replaceable);
    @IteratorPolyMod List<String> i = annotated(iteratorPoly);
    @Modifiable List<String> m = annotated(modifiable);
  }

  void unannotatedDoesNotPreserve(
      @Growable List<String> growable,
      @Shrinkable List<String> shrinkable,
      @Replaceable List<String> replaceable,
      @IteratorPolyMod List<String> iteratorPoly,
      @Modifiable List<String> modifiable) {
    // :: error: [assignment]
    @Growable List<String> g = unannotated(growable);
    // :: error: [assignment]
    @Shrinkable List<String> s = unannotated(shrinkable);
    // :: error: [assignment]
    @Replaceable List<String> r = unannotated(replaceable);
    // :: error: [assignment]
    @IteratorPolyMod List<String> i = unannotated(iteratorPoly);
    // :: error: [assignment]
    @Modifiable List<String> m = unannotated(modifiable);
  }

  // An argument whose type is a type variable is classified by the type variable's upper bound.
  <C extends @Growable Collection<String>> void typeVariableArgument(C growable) {
    @Growable List<String> g = annotated(growable);
  }

  <C extends Collection<String>> void typeVariableArgumentWithoutCapability(C unknown) {
    // :: error: [assignment]
    @Growable List<String> g = annotated(unknown);
  }

  // Despite the error on its declaration, the annotation has no effect on a call to such a method.
  void misplacedAnnotationHasNoEffect(@Growable List<String> growable) {
    annotatedVoid(growable);
    // :: error: [assignment]
    @Growable List<String> g = annotatedTwoArguments(growable, growable);
    // :: error: [assignment]
    @Growable List<String> v = annotatedVarargs(growable, growable);
  }
}
