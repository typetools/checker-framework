import java.util.List;

public class Issue6811 {
  boolean test(List<? extends Integer> y) {
    return 10 >= y.get(0);
  }
}
