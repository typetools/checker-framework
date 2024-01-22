import java.util.ArrayList;
import java.util.List;

public class RawSuper {
  static class MySuper<T> {
    public MySuper(List<Integer> p) {}

    void method(List<Integer> o) {}
  }

  @SuppressWarnings("unchecked") // raw extends
  static class SubRaw extends MySuper {
    public SubRaw() {
      super(new ArrayList<List<?>>());
    }
  }
}
