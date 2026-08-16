import java.util.function.Function;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// `java.util.function.Function` is declared `@Covariant(1)`, so
// `Function<String, @Untainted String>` is a subtype of `Function<String, @Tainted String>`.  Type
// inference must not require the two type arguments to have the same qualifier.
public class CovariantTypeArgumentInference {

  <T> void takeFunction(Function<T, @Tainted String> f) {}

  void use(Function<String, @Untainted String> f) {
    takeFunction(f);
  }
}
