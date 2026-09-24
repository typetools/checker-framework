import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.modifiability.qual.PreservesModifiability;

// The argument to a generic `@PreservesModifiability` method is itself a generic method invocation,
// whose type arguments are inferred from the enclosing invocation.  Computing the type of the
// argument requires the type of the enclosing invocation, which requires the type of the argument.
class PreservesModifiabilityPolyArgumentTest {

  @PreservesModifiability
  static <T> List<T> withoutDuplicates(Collection<T> values) {
    return new ArrayList<>(values);
  }

  void use() {
    withoutDuplicates(Arrays.asList(2, 3, 1));
  }
}
