import java.util.List;
import org.checkerframework.checker.index.qual.NonNegative;

// @inference-skip-test
// @below-java17-jdk-skip-test
public record Issue6100(List<@NonNegative Integer> bar) {

  public Issue6100 {
    List<@NonNegative Integer> b = bar;
    // :: error: (assignment)
    List<Integer> b2 = bar;
    if (bar.size() < 0) {
      throw new IllegalArgumentException();
    }
  }
}
