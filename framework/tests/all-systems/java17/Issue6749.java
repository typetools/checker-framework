import java.util.Optional;

// @below-java17-jdk-skip-test
public class Issue6749 {

  static Optional<Issue6749> getByNameOpt(String name) {
    return switch (name) {
      default -> Optional.empty();
    };
  }

  static Optional<Issue6749> getByNameOpt2(String name) {
    return switch (name) {
      case "1" -> Optional.empty();
      default -> Optional.empty();
    };
  }
}
