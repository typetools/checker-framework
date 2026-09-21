import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Replaceable;
import org.checkerframework.checker.modifiability.qual.SeqUngrowable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;

public class CollectionsModifiableTest {

  void testAddAll() {
    @Modifiable Collection<String> mod = new ArrayList<>();
    Collections.addAll(mod, "a", "b");

    @Unmodifiable Collection<String> unmod = Collections.unmodifiableCollection(mod);
    // :: error: [argument]
    Collections.addAll(unmod, "c");
  }

  void testAsLifoQueue() {
    @Modifiable Deque<String> mod = new LinkedList<>();
    Queue<String> q = Collections.asLifoQueue(mod);
    q.add("a");

    // there is no unmodifiable deque.
  }

  void testBinarySearch() {
    @Modifiable List<String> mod = new ArrayList<>();
    mod.add("a");
    Collections.binarySearch(mod, "a");

    @Unmodifiable List<String> unmod = Collections.unmodifiableList(mod);
    Collections.binarySearch(unmod, "a");
  }

  void testCheckedWrappers() {
    @Modifiable Collection<String> c = new ArrayList<>();
    @Modifiable Collection<String> checkedC = Collections.checkedCollection(c, String.class);
    checkedC.add("a");

    @Modifiable List<String> l = new ArrayList<>();
    @Modifiable List<String> checkedL = Collections.checkedList(l, String.class);
    checkedL.add("a");

    @Modifiable Map<String, String> m = new HashMap<>();
    @Modifiable Map<String, String> checkedM = Collections.checkedMap(m, String.class, String.class);
    checkedM.put("a", "b");

    @Modifiable Set<String> s = new HashSet<>();
    @Modifiable Set<String> checkedS = Collections.checkedSet(s, String.class);
    checkedS.add("a");

    // Checked wrappers should return modifiable views if the underlying collection is modifiable
    // But if we pass an unmodifiable collection, it should probably be an error or return
    // unmodifiable?
    // The JDK implementation just wraps it. The checker should probably enforce that the input is
    // modifiable if the output is used as modifiable.
    // However, checked wrappers are usually used to add runtime type safety to a modifiable
    // collection.
  }

  void testCopy() {
    @Modifiable List<String> dest = new ArrayList<>();
    dest.add("a");
    List<String> src = new ArrayList<>();
    src.add("b");

    Collections.copy(dest, src);

    @Unmodifiable List<String> unmodDest = Collections.unmodifiableList(dest);
    // :: error: [argument]
    Collections.copy(unmodDest, src);
  }

  void testDisjoint() {
    Collection<String> c1 = new ArrayList<>();
    Collection<String> c2 = new ArrayList<>();
    Collections.disjoint(c1, c2);
  }

  void testEmptyCollections() {
    @Unmodifiable List<String> l = Collections.emptyList();
    // :: error: [method.invocation]
    l.add("a");
    // :: error: [assignment]
    @Modifiable List<String> modL = Collections.emptyList();

    @Unmodifiable Set<String> s = Collections.emptySet();
    // :: error: [method.invocation]
    s.add("a");
    // :: error: [assignment]
    @Modifiable Set<String> modS = Collections.emptySet();

    @Unmodifiable Map<String, String> m = Collections.emptyMap();
    // :: error: [method.invocation]
    m.put("a", "b");
    // :: error: [assignment]
    @Modifiable Map<String, String> modM = Collections.emptyMap();

    @Unmodifiable Iterator<String> itor = Collections.emptyIterator();
    // :: error: [method.invocation]
    itor.remove();
    // :: error: [assignment]
    @Modifiable Iterator<String> modItor = Collections.emptyIterator();
  }

  void testFill() {
    @Modifiable List<String> list = new ArrayList<>();
    list.add("a");
    Collections.fill(list, "b");

    @Unmodifiable List<String> unmod = Collections.unmodifiableList(list);
    // :: error: [argument]
    Collections.fill(unmod, "c");
  }

  void testFrequency() {
    Collection<String> c = new ArrayList<>();
    Collections.frequency(c, "a");
  }

  void testIndexOfSubList() {
    List<String> src = new ArrayList<>();
    List<String> target = new ArrayList<>();
    Collections.indexOfSubList(src, target);
    Collections.lastIndexOfSubList(src, target);
  }

  void testListFromEnumeration() {
    Enumeration<String> e = Collections.emptyEnumeration();
    // Returns ArrayList which is modifiable
    @Modifiable ArrayList<String> l = Collections.list(e);
    l.add("a");
  }

  void testMinMax() {
    Collection<String> c = new ArrayList<>();
    Collections.max(c);
    Collections.min(c);
  }

  void testNCopies() {
    @Unmodifiable List<String> l = Collections.nCopies(5, "a");
    // :: error: [method.invocation]
    l.add("b");
    // :: error: [assignment]
    @Modifiable List<String> mod = Collections.nCopies(5, "a");
  }

  void testReplaceAll() {
    @Modifiable List<String> list = new ArrayList<>();
    list.add("a");
    Collections.replaceAll(list, "a", "b");

    @Unmodifiable List<String> unmod = Collections.unmodifiableList(list);
    // :: error: [argument]
    Collections.replaceAll(unmod, "a", "b");
  }

  void testReverse() {
    @Modifiable List<String> list = new ArrayList<>();
    list.add("a");
    Collections.reverse(list);

    @Unmodifiable List<String> unmod = Collections.unmodifiableList(list);
    // :: error: [argument]
    Collections.reverse(unmod);
  }

  void testRotate() {
    @Modifiable List<String> list = new ArrayList<>();
    list.add("a");
    Collections.rotate(list, 1);

    @Unmodifiable List<String> unmod = Collections.unmodifiableList(list);
    // :: error: [argument]
    Collections.rotate(unmod, 1);
  }

  void testShuffle() {
    @Modifiable List<String> list = new ArrayList<>();
    list.add("a");
    Collections.shuffle(list);
    Collections.shuffle(list, new Random());

    @Unmodifiable List<String> unmod = Collections.unmodifiableList(list);
    // :: error: [argument]
    Collections.shuffle(unmod);
    // :: error: [argument]
    Collections.shuffle(unmod, new Random());
  }

  void testSingleton() {
    @Unmodifiable Set<String> s = Collections.singleton("a");
    // :: error: [method.invocation]
    s.add("b");
    // :: error: [assignment]
    @Modifiable Set<String> modS = Collections.singleton("a");

    @Unmodifiable List<String> l = Collections.singletonList("a");
    // :: error: [method.invocation]
    l.add("b");
    // :: error: [assignment]
    @Modifiable List<String> modL = Collections.singletonList("a");

    @Unmodifiable Map<String, String> m = Collections.singletonMap("a", "b");
    // :: error: [method.invocation]
    m.put("c", "d");
    // :: error: [assignment]
    @Modifiable Map<String, String> modM = Collections.singletonMap("a", "b");
  }

  void testSort() {
    @Modifiable List<String> list = new ArrayList<>();
    list.add("a");
    Collections.sort(list);
    Collections.sort(list, Comparator.naturalOrder());

    @Unmodifiable List<String> unmod = Collections.unmodifiableList(list);
    // :: error: [argument]
    Collections.sort(unmod);
    // :: error: [argument]
    Collections.sort(unmod, Comparator.naturalOrder());
  }

  void testSwap() {
    @Modifiable List<String> mod = new ArrayList<>();
    mod.add("a");
    mod.add("b");
    Collections.swap(mod, 0, 1);

    @Unmodifiable List<String> unmod = Collections.unmodifiableList(mod);
    // :: error: [argument]
    Collections.swap(unmod, 0, 1);
  }

  void testSynchronizedWrappers() {
    // Synchronized wrappers pass through modifiability
    @Modifiable Collection<String> c = new ArrayList<>();
    @Modifiable Collection<String> syncC = Collections.synchronizedCollection(c);
    syncC.add("a");

    @Modifiable List<String> l = new ArrayList<>();
    @Modifiable List<String> syncL = Collections.synchronizedList(l);
    syncL.add("a");
  }

  void testUnmodifiableWrappers() {
    @Modifiable Collection<String> c = new ArrayList<>();
    @Unmodifiable Collection<String> unmodC = Collections.unmodifiableCollection(c);
    // :: error: [method.invocation]
    unmodC.add("a");
    // :: error: [assignment]
    @Modifiable Collection<String> modC = Collections.unmodifiableCollection(c);

    @Modifiable List<String> l = new ArrayList<>();
    @Unmodifiable List<String> unmodL = Collections.unmodifiableList(l);
    // :: error: [method.invocation]
    unmodL.add("a");
    // :: error: [assignment]
    @Modifiable List<String> modL = Collections.unmodifiableList(l);

    @Modifiable Set<String> s = new HashSet<>();
    @Unmodifiable Set<String> unmodS = Collections.unmodifiableSet(s);
    // :: error: [method.invocation]
    unmodS.add("a");
    // :: error: [assignment]
    @Modifiable Set<String> modS = Collections.unmodifiableSet(s);

    @Modifiable Map<String, String> m = new HashMap<>();
    @Unmodifiable Map<String, String> unmodM = Collections.unmodifiableMap(m);
    // :: error: [method.invocation]
    unmodM.put("a", "b");
    // :: error: [assignment]
    @Modifiable Map<String, String> modM = Collections.unmodifiableMap(m);

    @Growable @Shrinkable @Replaceable @SeqUngrowable SortedSet<String> ss = new TreeSet<>();
    @Unmodifiable SortedSet<String> unmodSS = Collections.unmodifiableSortedSet(ss);
    // :: error: [method.invocation]
    unmodSS.add("a");

    @Growable @Shrinkable @Replaceable @SeqUngrowable SortedMap<String, String> sm = new TreeMap<>();
    @Unmodifiable SortedMap<String, String> unmodSM = Collections.unmodifiableSortedMap(sm);
    // :: error: [method.invocation]
    unmodSM.put("a", "b");
  }
}
