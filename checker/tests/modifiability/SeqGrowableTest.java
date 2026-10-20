// @below-java21-jdk-skip-test

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.MaybeSeqGrowable;
import org.checkerframework.checker.modifiability.qual.SeqGrowable;
import org.checkerframework.checker.modifiability.qual.SeqUngrowable;

public class SeqGrowableTest {

  // :: error: [declaration.inconsistent.with.implements.clause]
  abstract static @SeqGrowable class UserDefinedSortedSet implements SortedSet<String> {}

  void constructorPositiveCases() {
    @SeqGrowable ArrayDeque<String> arrayDeque = new ArrayDeque<>();
    @SeqGrowable LinkedList<String> linkedList = new LinkedList<>();
    @SeqGrowable ArrayList<String> arrayList = new ArrayList<>();
    @SeqGrowable Vector<String> vector = new Vector<>();
    @SeqGrowable LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>();
    @SeqGrowable CopyOnWriteArrayList<String> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
    @SeqGrowable ConcurrentLinkedDeque<String> concurrentLinkedDeque = new ConcurrentLinkedDeque<>();
    @SeqGrowable LinkedBlockingDeque<String> linkedBlockingDeque = new LinkedBlockingDeque<>();

    arrayDeque.addFirst("a");
    linkedList.addLast("b");
    arrayList.addFirst("c");
    vector.addLast("d");
    linkedHashSet.addFirst("e");
    copyOnWriteArrayList.addLast("f");
    concurrentLinkedDeque.offerFirst("g");
    linkedBlockingDeque.offerLast("h");
  }

  void dequeOperations(@SeqGrowable Deque<String> deque) {
    deque.addFirst("a");
    deque.addLast("b");
    deque.offerFirst("c");
    deque.offerLast("d");
    deque.push("e");
  }

  void ordinaryGrowDoesNotImplySeqGrow(@Growable Deque<String> growOnly) {
    growOnly.add("a");
    // :: error: [method.invocation]
    growOnly.addFirst("b");
  }

  void maybeSeqGrowableIsRejected(
      SequencedCollection<String> unknown,
      @MaybeSeqGrowable List<String> maybeList,
      @MaybeSeqGrowable Deque<String> maybeDeque) {
    // :: error: [method.invocation]
    unknown.addFirst("a");

    // :: error: [method.invocation]
    maybeList.addLast("b");

    // :: error: [method.invocation]
    maybeDeque.offerFirst("c");
  }

  void sortedSetsAreSeqUngrowable() {
    @Growable TreeSet<String> growableTreeSet = new TreeSet<>();
    growableTreeSet.add("a");

    @SeqUngrowable TreeSet<String> seqUngrowableTreeSet = new TreeSet<>();
    @SeqUngrowable ConcurrentSkipListSet<String> seqUngrowableConcurrentSet = new ConcurrentSkipListSet<>();

    // :: error: [assignment]
    @SeqGrowable TreeSet<String> seqGrowableTreeSet = new TreeSet<>();

    @SeqGrowable ConcurrentSkipListSet<String> seqGrowableConcurrentSet =
        // :: error: [assignment]
        new ConcurrentSkipListSet<>();
  }
}
