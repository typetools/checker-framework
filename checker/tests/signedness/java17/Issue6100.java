import java.util.List;
import org.checkerframework.checker.index.qual.NonNegative;

// @infer-jaifs-skip-test The AFU's JAIF reading/writing libraries don't support records.
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
