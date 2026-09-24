import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.PreservesModifiability;
import org.checkerframework.checker.modifiability.qual.Replaceable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;

// The argument to a generic `@PreservesModifiability` method is itself a generic method invocation,
// whose type arguments are inferred from the enclosing invocation.  Computing the type of the
// argument requires the type of the enclosing invocation, which requires the type of the argument.
class PreservesModifiabilityPolyArgumentTest {

  @PreservesModifiability
  static <T> List<T> withoutDuplicates(Collection<T> values) {
    return new ArrayList<>(values);
  }

  static <U> @Growable List<U> makeGrowable() {
    throw new Error();
  }

  static <U> List<U> makeUnknown() {
    throw new Error();
  }

  void use() {
    withoutDuplicates(Arrays.asList(2, 3, 1));
  }

  // `Arrays.asList` returns a `@Replaceable @IteratorPolyMod` list.
  void preservesAsList() {
    @Replaceable List<Integer> r = withoutDuplicates(Arrays.asList(2, 3, 1));
    @IteratorPolyMod List<Integer> i = withoutDuplicates(Arrays.asList(2, 3, 1));
    // :: error: [assignment]
    @Growable List<Integer> g = withoutDuplicates(Arrays.asList(2, 3, 1));
    // :: error: [assignment]
    @Shrinkable List<Integer> s = withoutDuplicates(Arrays.asList(2, 3, 1));
  }

  void preservesGenericArgument() {
    @Growable List<String> g = withoutDuplicates(makeGrowable());
    // :: error: [assignment]
    @Shrinkable List<String> s = withoutDuplicates(makeGrowable());
    // :: error: [assignment]
    @Growable List<String> u = withoutDuplicates(makeUnknown());
  }
}
