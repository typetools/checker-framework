import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import org.checkerframework.checker.index.qual.GrowOnly;

/**
 * Test file for concurrent collections with GrowOnly annotations. Tests that @GrowOnly restrictions
 * work correctly with thread-safe collections.
 */
public class GrowOnlyConcurrentTest {

  void testConcurrentLinkedQueue() {
    @GrowOnly
    ConcurrentLinkedQueue<String> concurrentQueue = new @GrowOnly ConcurrentLinkedQueue<>();

    // These should be allowed - they grow the queue
    concurrentQueue.add("test1");
    concurrentQueue.offer("test2");

    // These should be errors - they shrink the queue
    // :: error: (method.invocation)
    concurrentQueue.poll();
    // :: error: (method.invocation)
    concurrentQueue.remove("test1");
    // :: error: (method.invocation)
    concurrentQueue.remove();
    // :: error: (method.invocation)
    concurrentQueue.clear();

    // Non-mutating operations should be allowed
    boolean isEmpty = concurrentQueue.isEmpty();
    int size = concurrentQueue.size();
    boolean contains = concurrentQueue.contains("test1");
    String peek = concurrentQueue.peek();
  }

  void testConcurrentLinkedDeque() {
    @GrowOnly
    ConcurrentLinkedDeque<String> concurrentDeque = new @GrowOnly ConcurrentLinkedDeque<>();

    // These should be allowed - they grow the deque
    concurrentDeque.addFirst("first");
    concurrentDeque.addLast("last");
    concurrentDeque.offerFirst("offerFirst");
    concurrentDeque.offerLast("offerLast");
    concurrentDeque.push("push");

    // These should be errors - they shrink the deque
    // :: error: (method.invocation)
    concurrentDeque.removeFirst();
    // :: error: (method.invocation)
    concurrentDeque.removeLast();
    // :: error: (method.invocation)
    concurrentDeque.pollFirst();
    // :: error: (method.invocation)
    concurrentDeque.pollLast();
    // :: error: (method.invocation)
    concurrentDeque.pop();
    // :: error: (method.invocation)
    concurrentDeque.remove("first");
    // :: error: (method.invocation)
    concurrentDeque.removeFirstOccurrence("first");
    // :: error: (method.invocation)
    concurrentDeque.removeLastOccurrence("last");
    // :: error: (method.invocation)
    concurrentDeque.clear();

    // Non-mutating peek operations should be allowed
    String peekFirst = concurrentDeque.peekFirst();
    String peekLast = concurrentDeque.peekLast();
  }

  void testCopyOnWriteArrayList() {
    @GrowOnly CopyOnWriteArrayList<String> cowList = new @GrowOnly CopyOnWriteArrayList<>();

    // These should be allowed - they grow the list
    cowList.add("item1");
    cowList.add(0, "item0");
    cowList.addAll(java.util.Arrays.asList("item2", "item3"));

    // These should be errors - they shrink the list
    // :: error: (method.invocation)
    cowList.remove("item1");
    // :: error: (method.invocation)
    cowList.remove(0);
    // :: error: (method.invocation)
    cowList.removeAll(java.util.Arrays.asList("item2"));
    // :: error: (method.invocation)
    cowList.retainAll(java.util.Arrays.asList("item1"));
    // :: error: (method.invocation)
    cowList.clear();

    // Set operation should be allowed (doesn't change size)
    if (!cowList.isEmpty()) {
      cowList.set(0, "newValue");
    }

    // Iterator should preserve restrictions
    java.util.Iterator<String> iter = cowList.iterator();
    iter.next();
    // :: error: (method.invocation)
    iter.remove();
  }

  void testCopyOnWriteArraySet() {
    @GrowOnly CopyOnWriteArraySet<String> cowSet = new @GrowOnly CopyOnWriteArraySet<>();

    // These should be allowed - they grow the set
    cowSet.add("item1");
    cowSet.addAll(java.util.Arrays.asList("item2", "item3"));

    // These should be errors - they shrink the set
    // :: error: (method.invocation)
    cowSet.remove("item1");
    // :: error: (method.invocation)
    cowSet.removeAll(java.util.Arrays.asList("item2"));
    // :: error: (method.invocation)
    cowSet.retainAll(java.util.Arrays.asList("item1"));
    // :: error: (method.invocation)
    cowSet.clear();

    // Iterator should preserve restrictions
    java.util.Iterator<String> iter = cowSet.iterator();
    iter.next();
    // :: error: (method.invocation)
    iter.remove();
  }

  void testArrayBlockingQueue() {
    @GrowOnly ArrayBlockingQueue<String> blockingQueue = new @GrowOnly ArrayBlockingQueue<>(10);

    // These should be allowed - they add elements (up to capacity)
    blockingQueue.add("item1");
    blockingQueue.offer("item2");

    try {
      blockingQueue.put("item3");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // These should be errors - they remove elements
    // :: error: (method.invocation)
    blockingQueue.poll();
    // :: error: (method.invocation)
    blockingQueue.remove();
    // :: error: (method.invocation)
    blockingQueue.remove("item1");
    // :: error: (method.invocation)
    blockingQueue.clear();

    try {
      // :: error: (method.invocation)
      blockingQueue.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  void testLinkedBlockingQueue() {
    @GrowOnly
    LinkedBlockingQueue<String> linkedBlockingQueue = new @GrowOnly LinkedBlockingQueue<>();

    // These should be allowed - they add elements
    linkedBlockingQueue.add("item1");
    linkedBlockingQueue.offer("item2");

    try {
      linkedBlockingQueue.put("item3");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // These should be errors - they remove elements
    // :: error: (method.invocation)
    linkedBlockingQueue.poll();
    // :: error: (method.invocation)
    linkedBlockingQueue.remove();
    // :: error: (method.invocation)
    linkedBlockingQueue.remove("item1");
    // :: error: (method.invocation)
    linkedBlockingQueue.clear();

    try {
      // :: error: (method.invocation)
      linkedBlockingQueue.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  void testLinkedBlockingDeque() {
    @GrowOnly
    LinkedBlockingDeque<String> linkedBlockingDeque = new @GrowOnly LinkedBlockingDeque<>();

    // These should be allowed - they add elements
    linkedBlockingDeque.addFirst("first");
    linkedBlockingDeque.addLast("last");
    linkedBlockingDeque.offerFirst("offerFirst");
    linkedBlockingDeque.offerLast("offerLast");

    try {
      linkedBlockingDeque.putFirst("putFirst");
      linkedBlockingDeque.putLast("putLast");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // These should be errors - they remove elements
    // :: error: (method.invocation)
    linkedBlockingDeque.removeFirst();
    // :: error: (method.invocation)
    linkedBlockingDeque.removeLast();
    // :: error: (method.invocation)
    linkedBlockingDeque.pollFirst();
    // :: error: (method.invocation)
    linkedBlockingDeque.pollLast();
    // :: error: (method.invocation)
    linkedBlockingDeque.clear();

    try {
      // :: error: (method.invocation)
      linkedBlockingDeque.takeFirst();
      // :: error: (method.invocation)
      linkedBlockingDeque.takeLast();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  void testPriorityBlockingQueue() {
    @GrowOnly
    PriorityBlockingQueue<Integer> priorityQueue = new @GrowOnly PriorityBlockingQueue<>();

    // These should be allowed - they add elements
    priorityQueue.add(1);
    priorityQueue.offer(2);

    try {
      priorityQueue.put(3);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // These should be errors - they remove elements
    // :: error: (method.invocation)
    priorityQueue.poll();
    // :: error: (method.invocation)
    priorityQueue.remove();
    // :: error: (method.invocation)
    priorityQueue.remove(1);
    // :: error: (method.invocation)
    priorityQueue.clear();

    try {
      // :: error: (method.invocation)
      priorityQueue.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  void testLinkedTransferQueue() {
    @GrowOnly LinkedTransferQueue<String> transferQueue = new @GrowOnly LinkedTransferQueue<>();

    // These should be allowed - they add elements
    transferQueue.add("item1");
    transferQueue.offer("item2");

    try {
      transferQueue.put("item3");
      transferQueue.transfer("item4");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // These should be errors - they remove elements
    // :: error: (method.invocation)
    transferQueue.poll();
    // :: error: (method.invocation)
    transferQueue.remove();
    // :: error: (method.invocation)
    transferQueue.remove("item1");
    // :: error: (method.invocation)
    transferQueue.clear();

    try {
      // :: error: (method.invocation)
      transferQueue.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  void testSynchronousQueue() {
    @GrowOnly SynchronousQueue<String> syncQueue = new @GrowOnly SynchronousQueue<>();

    // SynchronousQueue is special - it has zero capacity
    // So technically it never "grows" in the traditional sense
    // But offer/add operations should still be allowed
    boolean offered = syncQueue.offer("item");

    try {
      syncQueue.put("item2");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // These should be errors - they remove elements
    // :: error: (method.invocation)
    syncQueue.poll();
    // :: error: (method.invocation)
    syncQueue.remove();

    try {
      // :: error: (method.invocation)
      syncQueue.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  static class DelayedItem implements java.util.concurrent.Delayed {
    private final String item;
    private final long delayTime;

    DelayedItem(String item, long delayMs) {
      this.item = item;
      this.delayTime = System.currentTimeMillis() + delayMs;
    }

    @Override
    public long getDelay(java.util.concurrent.TimeUnit unit) {
      long diff = delayTime - System.currentTimeMillis();
      return unit.convert(diff, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(java.util.concurrent.Delayed other) {
      return Long.compare(this.delayTime, ((DelayedItem) other).delayTime);
    }
  }

  void testDelayQueue() {
    @GrowOnly DelayQueue<DelayedItem> delayQueue = new @GrowOnly DelayQueue<>();

    // These should be allowed - they add elements
    delayQueue.add(new DelayedItem("item1", 1000));
    delayQueue.offer(new DelayedItem("item2", 2000));

    try {
      delayQueue.put(new DelayedItem("item3", 3000));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // These should be errors - they remove elements
    // :: error: (method.invocation)
    delayQueue.poll();
    // :: error: (method.invocation)
    delayQueue.remove();
    // :: error: (method.invocation)
    delayQueue.clear();

    try {
      // :: error: (method.invocation)
      delayQueue.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  void testPolymorphicConcurrentAssignments() {
    @GrowOnly
    ConcurrentLinkedQueue<String> concurrentQueue = new @GrowOnly ConcurrentLinkedQueue<>();
    @GrowOnly CopyOnWriteArrayList<String> cowList = new @GrowOnly CopyOnWriteArrayList<>();

    // Assignment to more general interface types should preserve restrictions
    @GrowOnly Queue<String> queue = concurrentQueue;
    @GrowOnly List<String> list = cowList;
    @GrowOnly java.util.Collection<String> collection1 = concurrentQueue;
    @GrowOnly java.util.Collection<String> collection2 = cowList;

    // All should allow growth
    queue.offer("item");
    list.add("item");
    collection1.add("item");
    collection2.add("item");

    // None should allow shrinking
    // :: error: (method.invocation)
    queue.poll();
    // :: error: (method.invocation)
    list.remove(0);
    // :: error: (method.invocation)
    collection1.clear();
    // :: error: (method.invocation)
    collection2.clear();
  }
}
