import java.util.Optional;

public class OptionalOfNullTest {

  void test_optional_of_null_literal() {
    // :: error: (optional.of.null)
    Optional<String> somevar = Optional.of(null);
  }
}
