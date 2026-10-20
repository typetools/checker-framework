import java.util.Optional;

// @below-java17-jdk-skip-test
public class Issue6290 {

  public Optional<String> test(String param) {
    var first = Optional.ofNullable(param);
    var second = first.isPresent() ? first : Optional.ofNullable(param);
    return second;
  }
}
