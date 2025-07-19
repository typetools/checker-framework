import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.index.qual.GrowOnly;
import org.checkerframework.checker.index.qual.UncheckedShrinkable;

public class GrowOnlyJdkTest {

  void testAllowedCalls(@GrowOnly List<String> list) {
    list.add("new element");
  }

  void testForbiddenCalls(@GrowOnly List<String> list) {
    // :: error: (method.invocation)
    list.remove(0);
  }

  void testLocalVariable() {
    @SuppressWarnings("cast.unsafe.constructor.invocation")
    @GrowOnly
    List<String> localList = new @GrowOnly ArrayList<>();
    localList.add("hello");
    // :: error: (method.invocation)
    localList.clear();
  }

  void testUncheckedShrinkable() {
    // This assignment should be fine by default
    @SuppressWarnings("cast.unsafe.constructor.invocation")
    @UncheckedShrinkable
    List<String> list = new @UncheckedShrinkable ArrayList<>();
    list.add("hello");
    list.remove(0);
  }
}
