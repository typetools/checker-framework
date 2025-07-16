import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.index.qual.GrowOnly;
import org.checkerframework.checker.index.qual.UncheckedShrinkable;

public class GrowOnlyJdkTest {

  void testAllowedCalls(@GrowOnly List<String> list) {
    list.add("new element");
  }

  void testForbiddenCalls(@GrowOnly List<String> list) {
    // :: error: (growonly.collection.shrink)
    list.remove(0);
  }

  void testLocalVariable() {
    @GrowOnly List<String> localList = new ArrayList<>();
    localList.add("hello");
    // :: error: (growonly.collection.shrink)
    localList.clear();
  }

  void testUncheckedShrinkable() {
    // This assignment should be fine by default
    @UncheckedShrinkable List<String> list = new ArrayList<>();
    list.add("hello");
    // This call should be allowed without error
    list.remove(0);
  }
}
