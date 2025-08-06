import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.checkerframework.checker.index.qual.GrowOnly;

public class GrowOnlyViewTest {

  void testViewCollection(@GrowOnly List<String> list) {
    Collection<String> cc = Collections.checkedCollection(list, String.class);
    // :: error: (method.invocation)
    cc.clear();

    Collection<String> syncC = Collections.synchronizedCollection(list);
    // :: error: (method.invocation)
    syncC.clear();

    List<String> subList = list.subList(2, 4);
    // :: error: (method.invocation)
    subList.clear();
  }

  void testIterator(@GrowOnly List<String> list) {
    Iterator<String> itor = list.iterator();
    // :: error: (method.invocation)
    itor.next();
    // :: error: (method.invocation)
    itor.remove();
  }
}
