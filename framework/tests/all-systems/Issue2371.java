@SuppressWarnings("all")
public class Issue2371<T extends Issue2371<T>> {
  void method(Issue2371<? extends Object> i) {
    other(i);
  }

  void other(Issue2371<?> e) {}
}
