import org.checkerframework.checker.nullness.qual.*;

public class FlowAssignment {

  void test() {
    @NonNull String s = "foo";

    String t = s;
    t.startsWith("f");
  }
}
