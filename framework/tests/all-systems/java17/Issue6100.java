import java.util.List;
import org.checkerframework.checker.index.qual.NonNegative;

// @inference-skip-test
// @below-java17-jdk-skip-test
@SuppressWarnings("signedness")
public record Issue6100(List<@NonNegative Integer> bar) {

  public Issue6100 {
    if (bar.size() < 0) {
      throw new IllegalArgumentException();
    }
  }
}
