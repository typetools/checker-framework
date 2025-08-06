// @below-java21-jdk-skip-test

import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.index.qual.GrowOnly;

public class GrowOnlyJdk21Test {

  List<String> list2 = Arrays.asList("hello");

  void testCollection(@GrowOnly List<String> list) {
    // SequencedCollection was added in Java 21 and is not yet in the annotated JDK.
    /*
    SequencedCollection<String> sc = list;
    // :: error: (method.invocation)
    sc.removeFirst("hello");
    // :: error: (method.invocation)
    sc.removeLast("hello");
    // :: error: (method.invocation)
    sc.clear();
    // :: error: (method.invocation)
    sc.remove("hello");
    // :: error: (method.invocation)
    sc.removeAll(list2);
    // :: error: (method.invocation)
    sc.removeIf(s -> s.equals("hello"));
    // :: error: (method.invocation)
    sc.retainAll(list2);
    */

  }

  void testViewCollection(@GrowOnly List<String> list) {
    // SequencedCollection was added in Java 21 and is not yet in the annotated JDK.
    /*
    SequencedCollection<String> sc2 = sc.reversed();
    // :: error: (method.invocation)
    sc2.removeFirst("hello");
    // :: error: (method.invocation)
    sc2.removeLast("hello");
    */
  }
}
