// @below-java21-jdk-skip-test

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.MaybeSeqGrowable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Replaceable;
import org.checkerframework.checker.modifiability.qual.SeqGrowable;
import org.checkerframework.checker.modifiability.qual.SeqUngrowable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;

public class SeqGrowableMapTest {

  // :: error: [declaration.inconsistent.with.implements.clause]
  abstract static @SeqGrowable class UserDefinedSortedMap implements SortedMap<String, Integer> {}

  void linkedHashMapSupportsSequencedMapGrow() {
    @Modifiable LinkedHashMap<String, Integer> modifiable = new LinkedHashMap<>();
    modifiable.putFirst("a", 1);
    modifiable.putLast("b", 2);

    @SeqGrowable LinkedHashMap<String, Integer> seqGrowable = new LinkedHashMap<>();
    seqGrowable.putFirst("c", 3);
    seqGrowable.putLast("d", 4);

    @SeqGrowable SequencedMap<String, Integer> sequenced = new LinkedHashMap<>();
    sequenced.putFirst("e", 5);
    sequenced.putLast("f", 6);
  }

  void ordinaryMapsCanStillUseModifiable() {
    @Modifiable HashMap<String, Integer> map = new HashMap<>();
    map.put("a", 1);
    map.remove("a");
    map.put("b", 2);
    map.replace("b", 3);
  }

  void maybeSeqGrowableIsRejected(
      SequencedMap<String, Integer> unknown,
      @MaybeSeqGrowable SequencedMap<String, Integer> maybeSequenced) {
    // :: error: [method.invocation]
    unknown.putFirst("a", 1);
    // :: error: [method.invocation]
    maybeSequenced.putLast("b", 2);
  }

  void ordinaryGrowAndReplaceDoNotImplySeqGrow(
      @Growable @Replaceable SequencedMap<String, Integer> growReplace) {
    growReplace.put("a", 1);
    // :: error: [method.invocation]
    growReplace.putFirst("b", 2);
    // :: error: [method.invocation]
    growReplace.putLast("c", 3);
  }

  void sortedMapsAreSeqUngrowable() {
    @Growable @Shrinkable @Replaceable @SeqUngrowable TreeMap<String, Integer> treeMap = new TreeMap<>();
    treeMap.put("a", 1);
    treeMap.remove("a");
    treeMap.put("b", 2);
    treeMap.replace("b", 3);
    // :: error: [method.invocation]
    treeMap.putFirst("c", 4);

    @Growable @Shrinkable @Replaceable @SeqUngrowable ConcurrentSkipListMap<String, Integer> concurrentSkipListMap = new ConcurrentSkipListMap<>();
    concurrentSkipListMap.put("a", 1);
    concurrentSkipListMap.remove("a");
    concurrentSkipListMap.put("b", 2);
    concurrentSkipListMap.replace("b", 3);
    // :: error: [method.invocation]
    concurrentSkipListMap.putLast("c", 4);

    // :: error: [assignment]
    @SeqGrowable TreeMap<String, Integer> seqGrowableTreeMap = new TreeMap<>();

    // :: error: [assignment]
    @Modifiable TreeMap<String, Integer> modifiableTreeMap = new TreeMap<>();

    @SeqGrowable ConcurrentSkipListMap<String, Integer> seqGrowableConcurrentSkipListMap =
        // :: error: [assignment]
        new ConcurrentSkipListMap<>();

    @Modifiable ConcurrentSkipListMap<String, Integer> modifiableConcurrentSkipListMap =
        // :: error: [assignment]
        new ConcurrentSkipListMap<>();
  }
}
