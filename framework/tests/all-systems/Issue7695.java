import java.util.Optional;

@SuppressWarnings("optional.parameter") // true positive.
public class Issue7695 {
  void test(Optional<Object> optional) {
    Optional<String> x = optional.flatMap((Object unused) -> Optional.of(""));
  }
}
