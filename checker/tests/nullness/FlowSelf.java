import org.checkerframework.checker.nullness.qual.*;

public class FlowSelf {

  void test(@Nullable String s) {

    if (s == null) {
      return;
    }
    // :: warning: (nulltest.redundant)
    assert s != null;

    s = s.substring(1);
  }
}
