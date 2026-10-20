import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;

// Tests that storing a modifiable subtype in a less-capable supertype does not permanently discard
// modifiability dimensions that the subtype supports.
class SubtypeCapabilityPreservationTest {

  void listIteratorThroughIterator() {
    List<String> list = new ArrayList<>();
    @Modifiable ListIterator<String> listIterator = list.listIterator();
    Iterator<String> iterator = listIterator;
    @Modifiable ListIterator<String> listIterator2 = (ListIterator<String>) iterator;
    listIterator2.add("added");
    listIterator2.set("replaced");
    listIterator2.remove();

    List<String> unmodList = Collections.unmodifiableList(new ArrayList<>());
    @Unmodifiable ListIterator<String> listIterator3 = unmodList.listIterator();
    Iterator<String> iterator2 = listIterator3;
    // :: error: [assignment]
    @Modifiable ListIterator<String> listIterator4 = (ListIterator<String>) iterator2;
  }

  void linkedListThroughQueue() {
    @Modifiable LinkedList<String> linkedList = new LinkedList<>();
    @Modifiable Queue<String> queue = linkedList;
    @Modifiable LinkedList<String> linkedList2 = (LinkedList<String>) queue;
    linkedList2.add("added");
    linkedList2.set(0, "replaced");
    linkedList2.remove();
  }

  void arrayListThroughCollection() {
    @Modifiable List<String> arrayList = new ArrayList<>();
    @Modifiable Collection<String> collection = arrayList;
    @Modifiable List<String> arrayList2 = (ArrayList<String>) collection;
    arrayList2.add("added");
    arrayList2.set(0, "replaced");
    arrayList2.remove(0);
  }
}
