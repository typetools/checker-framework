import java.util.AbstractCollection;
import java.util.AbstractSequentialList;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;
import org.checkerframework.checker.modifiability.qual.Unshrinkable;

public class IteratorPrecisionTest {

  enum TestEnum {
    A,
    B
  }

  @IteratorPolyMod List<String> list;

  void iteratorChecker() {
    // this should not be allowed because CopyOnWriteArrayList's iterator does not preserve remove
    // :: error: [assignment]
    list = new CopyOnWriteArrayList<>();
    @Shrinkable Iterator<String> iterator = list.iterator();
  }

  void arrayListIterator() {
    // ok
    ArrayList<String> list = new ArrayList<>();
    @Shrinkable Iterator<String> iterator = list.iterator();
    iterator.remove();

    // should still be ok
    List<String> list2 = new ArrayList<>();
    Iterator<String> iterator2 = list2.iterator();
    iterator2.remove();
  }

  void copyOnWriteArrayListIterator() {
    // as expected,
    CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
    @Unshrinkable Iterator<String> iterator = list.iterator();

    // store CopyOnWriteArrayList as a list, the iterator is @MaybeShrinkable.
    List<String> list2 = new CopyOnWriteArrayList<>();
    // The iterator below is @MaybeShrinkable.  List.iterator() declares no shrink qualifier, so
    // refineIteratorReturnType uses the receiver: a @Shrinkable receiver that is also
    // @IteratorPolyMod yields a @Shrinkable result, and every other receiver yields the top
    // qualifier.  Here the receiver's static type is List, which is not @IteratorPolyMod.
    // TODO: Consider giving CopyOnWriteArrayList an @Unshrinkable iterator, so that the result is
    // @Unshrinkable rather than @MaybeShrinkable even through a List-typed reference.
    Iterator<String> iterator2 = list2.iterator();
    // :: error: [method.invocation]
    iterator2.remove();
  }

  void copyOnWriteArrayListListIterator() {
    CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
    @Unmodifiable ListIterator<String> iterator = list.listIterator();
    @Unmodifiable ListIterator<String> indexedIterator = list.listIterator(0);

    List<String> list2 = new CopyOnWriteArrayList<>();
    // default to be unknown mod.
    ListIterator<String> iterator2 = list2.listIterator();
    ListIterator<String> indexedIterator2 = list2.listIterator(0);
  }

  void arryaListListIterator() {
    ArrayList<String> list = new ArrayList<>();
    @Modifiable ListIterator<String> iterator = list.listIterator();
    iterator.remove();
    iterator.set("c");
    iterator.add("d");
    @Modifiable ListIterator<String> indexedIterator = list.listIterator(0);

    List<String> list2 = new ArrayList<>();
    @Modifiable ListIterator<String> iterator2 = list2.listIterator();
    @Modifiable ListIterator<String> indexedIterator2 = list2.listIterator(0);
  }

  // ArrayList declares `@Growable ListIterator<E> listIterator()`, but that result has the
  // capability only when the receiver has it.  A parameter whose type is written as plain
  // `ArrayList` is @MaybeGrowable, so its list iterator is @MaybeGrowable too.
  void listIteratorOfMaybeGrowableArrayList(ArrayList<String> list) {
    ListIterator<String> iterator = list.listIterator();
    // :: error: [method.invocation]
    iterator.add("a");
  }

  void UnmodListIterator() {
    List<String> list = List.of("a", "b");
    @Unshrinkable Iterator<String> iterator = list.iterator();
  }

  void KeySetIterator() {
    LinkedHashMap<String, String> map = new LinkedHashMap<>();
    map.put("a", "1");
    map.put("b", "2");
    // ok
    map.keySet().iterator().remove();

    @Shrinkable Set<String> keys = map.keySet();
    @Shrinkable Iterator<String> iterator2 = keys.iterator();

    @Modifiable Map<String, String> map2 = new LinkedHashMap<>();
    map2.put("a", "1");
    map2.put("b", "2");

    // should be ok
    map2.keySet().iterator().remove();

    // ok
    @Shrinkable Set<String> keys2 = map2.keySet();
    @Shrinkable Iterator<String> iter2 = keys2.iterator();
  }

  void setIteratorPolyMod() {
    Set<String> hashSet = new HashSet<>();
    @Shrinkable Iterator<String> hashSetIterator = hashSet.iterator();
    hashSetIterator.remove();

    Set<String> treeSet = new TreeSet<>();
    @Shrinkable Iterator<String> treeSetIterator = treeSet.iterator();
    treeSetIterator.remove();
  }

  void dequeIteratorPolyMod() {
    Deque<String> arrayDeque = new ArrayDeque<>();
    @Shrinkable Iterator<String> arrayDequeIterator = arrayDeque.iterator();
    arrayDequeIterator.remove();

    Deque<String> linkedList = new LinkedList<>();
    @Shrinkable Iterator<String> linkedListIterator = linkedList.iterator();
    linkedListIterator.remove();
  }

  void queueIteratorPolyMod() {
    Queue<String> priorityQueue = new PriorityQueue<>();
    @Shrinkable Iterator<String> priorityQueueIterator = priorityQueue.iterator();
    priorityQueueIterator.remove();

    BlockingQueue<String> arrayBlockingQueue = new ArrayBlockingQueue<>(1);
    @Shrinkable Iterator<String> arrayBlockingQueueIterator = arrayBlockingQueue.iterator();
    arrayBlockingQueueIterator.remove();

    BlockingQueue<String> linkedBlockingQueue = new LinkedBlockingQueue<>();
    @Shrinkable Iterator<String> linkedBlockingQueueIterator = linkedBlockingQueue.iterator();
    linkedBlockingQueueIterator.remove();

    TransferQueue<String> linkedTransferQueue = new LinkedTransferQueue<>();
    @Shrinkable Iterator<String> linkedTransferQueueIterator = linkedTransferQueue.iterator();
    linkedTransferQueueIterator.remove();
  }

  void factoryIteratorPolyMod() {
    Set<TestEnum> enumSet = EnumSet.of(TestEnum.A, TestEnum.B);
    @Shrinkable Iterator<TestEnum> enumSetIterator = enumSet.iterator();
    enumSetIterator.remove();

    Set<TestEnum> enumSetCopy = EnumSet.copyOf(enumSet);
    @Shrinkable Iterator<TestEnum> enumSetCopyIterator = enumSetCopy.iterator();
    enumSetCopyIterator.remove();

    Enumeration<String> enumeration = Collections.enumeration(List.of("a", "b"));
    List<String> list = Collections.list(enumeration);
    @Shrinkable Iterator<String> listIterator = list.iterator();
    listIterator.remove();
  }

  void mapViewIteratorsPreserveRemove() {
    HashMap<String, String> hashMap = new HashMap<>();
    @Shrinkable Iterator<String> hashMapKeys = hashMap.keySet().iterator();
    hashMapKeys.remove();
    @Shrinkable Iterator<String> hashMapValues = hashMap.values().iterator();
    hashMapValues.remove();
    @Shrinkable Iterator<Map.Entry<String, String>> hashMapEntries = hashMap.entrySet().iterator();
    hashMapEntries.remove();

    LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
    @Shrinkable Iterator<String> linkedKeys = linkedHashMap.keySet().iterator();
    linkedKeys.remove();
    @Shrinkable Iterator<String> linkedValues = linkedHashMap.values().iterator();
    linkedValues.remove();
    @Shrinkable Iterator<Map.Entry<String, String>> linkedEntries = linkedHashMap.entrySet().iterator();
    linkedEntries.remove();

    EnumMap<TestEnum, String> enumMap = new EnumMap<>(TestEnum.class);
    @Shrinkable Iterator<TestEnum> enumKeys = enumMap.keySet().iterator();
    enumKeys.remove();
    @Shrinkable Iterator<String> enumValues = enumMap.values().iterator();
    enumValues.remove();
    @Shrinkable Iterator<Map.Entry<TestEnum, String>> enumEntries = enumMap.entrySet().iterator();
    enumEntries.remove();

    ConcurrentHashMap<String, String> concurrentHashMap = new ConcurrentHashMap<>();
    @Shrinkable Iterator<String> concurrentKeys = concurrentHashMap.keySet().iterator();
    concurrentKeys.remove();
    @Shrinkable Iterator<String> concurrentValues = concurrentHashMap.values().iterator();
    concurrentValues.remove();
    @Shrinkable Iterator<Map.@Modifiable Entry<String, String>> concurrentEntries =
        concurrentHashMap.entrySet().iterator();
    concurrentEntries.remove();

    ConcurrentSkipListMap<String, String> skipListMap = new ConcurrentSkipListMap<>();
    @Shrinkable Iterator<String> skipListKeys = skipListMap.keySet().iterator();
    skipListKeys.remove();
    @Shrinkable Iterator<String> skipListValues = skipListMap.values().iterator();
    skipListValues.remove();
    @Shrinkable Iterator<Map.@Unmodifiable Entry<String, String>> skipListEntries =
        skipListMap.entrySet().iterator();
    skipListEntries.remove();
  }

  void unmodifiableMapViewIteratorsDoNotPreserveRemove() {
    @Unmodifiable Map<String, String> map = Map.of("a", "b");
    @Unshrinkable Iterator<String> keyIterator = map.keySet().iterator();
    @Unshrinkable Iterator<String> valueIterator = map.values().iterator();
    @Unshrinkable Iterator<Map.@Unmodifiable Entry<String, String>> entryIterator = map.entrySet().iterator();
  }

  void unmodifiableMapWrapperViewIteratorsDoNotPreserveRemove() {
    HashMap<String, String> backing = new HashMap<>();
    @Unmodifiable Map<String, String> map = Collections.unmodifiableMap(backing);
    @Unshrinkable Iterator<String> keyIterator = map.keySet().iterator();
    @Unshrinkable Iterator<String> valueIterator = map.values().iterator();
    @Unshrinkable Iterator<Map.@Unmodifiable Entry<String, String>> entryIterator = map.entrySet().iterator();
  }

  void iteratorDependentCollectionMethodsRequireIteratorPolyMod(
      @Modifiable IteratorRemoveUnsupportedCollection collection,
      @Modifiable IteratorRemoveUnsupportedSet set) {
    // :: error: [method.invocation]
    collection.remove("a");
    // :: error: [method.invocation]
    collection.removeAll(List.of("a"));
    // :: error: [method.invocation]
    collection.retainAll(List.of("a"));
    // :: error: [method.invocation]
    collection.clear();
    // :: error: [method.invocation]
    collection.removeIf(s -> true);

    // :: error: [method.invocation]
    set.removeAll(List.of("a"));
  }

  void iteratorDependentListMethodsRequireIteratorPolyMod(
      @Modifiable IteratorMutationUnsupportedList list) {
    // :: error: [method.invocation]
    list.set(0, "b");
    // :: error: [method.invocation]
    list.add(0, "b");
    // :: error: [method.invocation]
    list.remove(0);
    // :: error: [method.invocation]
    list.addAll(0, List.of("b"));
    // :: error: [method.invocation]
    list.replaceAll(s -> s + "!");
    // :: error: [method.invocation]
    list.sort(null);

    // :: error: [argument]
    Collections.sort(list);
    // :: error: [argument]
    Collections.sort(list, String::compareTo);
    // :: error: [argument]
    Collections.reverse(list);
    // :: error: [argument]
    Collections.shuffle(list);
    // :: error: [argument]
    Collections.shuffle(list, new Random());
    // :: error: [argument]
    Collections.fill(list, "b");
    // :: error: [argument]
    Collections.copy(list, List.of("b"));
    // :: error: [argument]
    Collections.rotate(list, 1);
    // :: error: [argument]
    Collections.replaceAll(list, "a", "b");
  }

  void copyOnWriteDirectOverridesRemainUsableButInheritedIteratorBasedApisDoNot() {
    CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
    list.add("a");
    list.removeIf(s -> false);
    list.replaceAll(s -> s + "!");
    list.sort(null);
    list.clear();

    // :: error: [argument]
    Collections.sort(list);
    // :: error: [argument]
    Collections.reverse(list);

    List<String> asList = list;
    // :: error: [method.invocation]
    asList.sort(null);
    // :: error: [method.invocation]
    asList.replaceAll(s -> s);
  }

  static class IteratorRemoveUnsupportedCollection extends AbstractCollection<String> {
    @Override
    public Iterator<String> iterator() {
      return new Iterator<>() {
        private boolean hasNext = true;

        @Override
        public boolean hasNext() {
          return hasNext;
        }

        @Override
        public String next() {
          if (!hasNext) {
            throw new NoSuchElementException();
          }
          hasNext = false;
          return "a";
        }
      };
    }

    @Override
    public int size() {
      return 1;
    }
  }

  static class IteratorRemoveUnsupportedSet extends AbstractSet<String> {
    @Override
    public Iterator<String> iterator() {
      return List.of("a").iterator();
    }

    @Override
    public int size() {
      return 1;
    }
  }

  static class IteratorMutationUnsupportedList extends AbstractSequentialList<String> {
    @Override
    public ListIterator<String> listIterator(int index) {
      return new ListIterator<>() {
        private int cursor = index;

        @Override
        public boolean hasNext() {
          return cursor < 1;
        }

        @Override
        public String next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          cursor++;
          return "a";
        }

        @Override
        public boolean hasPrevious() {
          return cursor > 0;
        }

        @Override
        public String previous() {
          if (!hasPrevious()) {
            throw new NoSuchElementException();
          }
          cursor--;
          return "a";
        }

        @Override
        public int nextIndex() {
          return cursor;
        }

        @Override
        public int previousIndex() {
          return cursor - 1;
        }

        @Override
        // :: error: [override.receiver]
        public void remove() {
          throw new UnsupportedOperationException();
        }

        @Override
        // :: error: [override.receiver]
        public void set(String e) {
          throw new UnsupportedOperationException();
        }

        @Override
        // :: error: [override.receiver]
        public void add(String e) {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public int size() {
      return 1;
    }
  }
}
