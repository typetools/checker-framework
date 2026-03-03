@SuppressWarnings({"unchecked", "all"}) // check for crashes
public class Misc<T> {
  Misc<? super T> forward;

  public <E extends T> E min(E a, E b) {
    return forward.max(a, b);
  }

  public <F extends T> F min(F a, F b, F c, F... rest) {
    return forward.max(a, b, c, rest);
  }

  public <G extends T> G max(G a, G b) {
    return forward.min(a, b);
  }

  public <H extends T> H max(H a, H b, H c, H... rest) {
    return forward.min(a, b, c, rest);
  }
}
