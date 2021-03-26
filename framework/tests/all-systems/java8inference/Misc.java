@SuppressWarnings({"unchecked", "all"}) // check for crashes
public class Misc<T> {
  Misc<? super T> forward;

  public <E extends T> E min(E a, E b) {
    return forward.max(a, b);
  }

  public <E extends T> E min(E a, E b, E c, E... rest) {
    return forward.max(a, b, c, rest);
  }

  public <E extends T> E max(E a, E b) {
    return forward.min(a, b);
  }

  public <E extends T> E max(E a, E b, E c, E... rest) {
    return forward.min(a, b, c, rest);
  }
}
