import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.checkerframework.checker.index.qual.GrowOnly;

public class GrowOnlySupertypeTest {

  List<String> list2 = Arrays.asList("hello");

  void testList(@GrowOnly List<String> list) {
    Collection<String> c = list;
    // :: error: (method.invocation)
    c.remove("hello");
    // :: error: (method.invocation)
    c.removeAll(list2);
    // :: error: (method.invocation)
    c.removeIf(s -> s.equals("hello"));
    // :: error: (method.invocation)
    c.retainAll(list2);
    // :: error: (method.invocation)
    c.clear();

    Iterable<String> ible = list;
    Iterator<String> itor = ible.iterator();
    itor.next();
    // :: error: (method.invocation)
    itor.remove();
  }

  void testAbstractCollection(@GrowOnly AbstractCollection<String> ac) {
    // :: error: (method.invocation)
    ac.remove("hello");
    // :: error: (method.invocation)
    ac.removeAll(list2);
    // :: error: (method.invocation)
    ac.retainAll(list2);
    // :: error: (method.invocation)
    ac.clear();
  }

  void testArrayList(@GrowOnly ArrayList<String> list) {
    AbstractCollection<String> ac = list;
    // :: error: (method.invocation)
    ac.remove("hello");
    // :: error: (method.invocation)
    ac.removeAll(list2);
    // :: error: (method.invocation)
    ac.retainAll(list2);
    // :: error: (method.invocation)
    ac.clear();
  }

  void testDeque(@GrowOnly Deque<String> d) {
    // :: error: (method.invocation)
    d.remove("hello");
    // :: error: (method.invocation)
    d.removeAll(list2);
    // :: error: (method.invocation)
    d.removeIf(s -> s.equals("hello"));
    // :: error: (method.invocation)
    d.retainAll(list2);
    // :: error: (method.invocation)
    d.clear();
  }

  void testLinkedList(@GrowOnly LinkedList<String> list) {
    Queue<String> q = list;
    // :: error: (method.invocation)
    q.remove("hello");
    // :: error: (method.invocation)
    q.removeAll(list2);
    // :: error: (method.invocation)
    q.removeIf(s -> s.equals("hello"));
    // :: error: (method.invocation)
    q.retainAll(list2);
    // :: error: (method.invocation)
    q.clear();
    // :: error: (method.invocation)
    q.poll();

    Deque<String> d = list;
    // :: error: (method.invocation)
    d.remove("hello");
    // :: error: (method.invocation)
    d.removeAll(list2);
    // :: error: (method.invocation)
    d.removeIf(s -> s.equals("hello"));
    // :: error: (method.invocation)
    d.retainAll(list2);
    // :: error: (method.invocation)
    d.clear();

    // :: error: (method.invocation)
    d.poll();
    // :: error: (method.invocation)
    d.pollFirst();
    // :: error: (method.invocation)
    d.pollLast();
    // :: error: (method.invocation)
    d.pop();
    // :: error: (method.invocation)
    d.remove();
    // :: error: (method.invocation)
    d.removeFirst();
    // :: error: (method.invocation)
    d.removeFirstOccurrence("hello");
    // :: error: (method.invocation)
    d.removeLast();
    // :: error: (method.invocation)
    d.removeLastOccurrence("hello");
  }
}
