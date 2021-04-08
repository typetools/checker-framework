import java.util.Map;
import java.util.Optional;

public class Issue4372 {
  private Optional<? extends Map<String, Integer>> o;

  public Issue4372() {
    this.o = Optional.<Map<String, Integer>>empty();
  }

  void f(String k, Optional<Integer> x) {
    if (!o.isPresent() || !o.get().containsKey(k)) {
      return;
    }
    Integer y = o.get().get(k);
    if (!x.isPresent()) {
      return;
    }
  }
}
