import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.index.qual.GrowOnly;

public class GrowOnlyJdkTest {

  void testAllowedCalls(@GrowOnly List<String> list) {
    // valid call, because add() does not have a @Shrinkable annotation.
    list.add("new element");
  }

  void testForbiddenCalls(@GrowOnly List<String> list) {
    // :: error: (mutable.collection.shrink)
    list.remove(0);
  }

  void testLocalVariable() {
    @GrowOnly List<String> localList = new ArrayList<>();
    localList.add("hello");
    // :: error: (mutable.collection.shrink)
    localList.clear();
  }
}
