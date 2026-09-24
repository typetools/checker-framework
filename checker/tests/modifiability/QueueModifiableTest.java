import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import org.checkerframework.checker.modifiability.qual.Modifiable;

// Tests for Queue, Deque, BlockingQueue, etc. mutating and read-only methods with modifiability
// qualifiers.
// Per request, we only test Modifiable queues as creating truly unmodifiable queues is not
// standard.
public class QueueModifiableTest {

  void testQueue() {
    @Modifiable Queue<String> mod = new LinkedList<>();
    // Mutating
    mod.add("a");
    mod.offer("b");
    mod.remove();
    mod.poll();
    mod.clear(); // inherited from Collection

    // Read-only
    mod.element();
    mod.peek();
  }

  void testDeque() {
    @Modifiable Deque<String> mod = new ArrayDeque<>();
    // Mutating
    mod.addFirst("a");
    mod.addLast("b");
    mod.offerFirst("c");
    mod.offerLast("d");
    mod.removeFirst();
    mod.removeLast();
    mod.pollFirst();
    mod.pollLast();
    mod.removeFirstOccurrence("a");
    mod.removeLastOccurrence("b");
    mod.push("e");
    mod.pop();

    // Read-only
    mod.getFirst();
    mod.getLast();
    mod.peekFirst();
    mod.peekLast();
    mod.descendingIterator();
  }

  void testBlockingQueue() throws InterruptedException {
    @Modifiable BlockingQueue<String> mod = new ArrayBlockingQueue<>(10);
    // Mutating
    mod.put("a");
    mod.offer("b", 1, TimeUnit.SECONDS);
    mod.take();
    mod.poll(1, TimeUnit.SECONDS);
    mod.drainTo(new LinkedList<>());
    mod.drainTo(new LinkedList<>(), 10);

    // Read-only
    mod.remainingCapacity();
  }

  void testTransferQueue() throws InterruptedException {
    @Modifiable TransferQueue<String> mod = new LinkedTransferQueue<>();
    // Mutating
    mod.transfer("a");
    mod.tryTransfer("b");
    mod.tryTransfer("c", 1, TimeUnit.SECONDS);

    // Read-only
    mod.hasWaitingConsumer();
    mod.getWaitingConsumerCount();
  }

  void testBlockingDeque() throws InterruptedException {
    @Modifiable BlockingDeque<String> mod = new LinkedBlockingDeque<>();
    // Mutating
    mod.putFirst("a");
    mod.putLast("b");
    mod.offerFirst("c", 1, TimeUnit.SECONDS);
    mod.offerLast("d", 1, TimeUnit.SECONDS);
    mod.takeFirst();
    mod.takeLast();
    mod.pollFirst(1, TimeUnit.SECONDS);
    mod.pollLast(1, TimeUnit.SECONDS);
  }

  void testArrayDeque() {
    @Modifiable ArrayDeque<String> mod = new ArrayDeque<>();
    mod.add("a");
    mod.clone();
  }

  void testPriorityQueue() {
    @Modifiable PriorityQueue<String> mod = new PriorityQueue<>();
    mod.add("a");
    Comparator<? super String> c = mod.comparator();
  }

  void testUnmodifiableWrapper() {
    Queue<String> queue = new LinkedList<>();
    queue.add("initial");

    // Case 1: Modifying an unmodifiable Collection view of a Queue
    Collection<String> unmodifiableQueue = Collections.unmodifiableCollection(queue);
    // :: error: [method.invocation]
    unmodifiableQueue.add("This will fail");

    // Case 2: Iterator remove on Unmodifiable Collection
    Collection<String> unmodifiableQueue2 = Collections.unmodifiableCollection(queue);
    Iterator<String> it = unmodifiableQueue2.iterator();
    if (it.hasNext()) {
      it.next();
      // :: error: [method.invocation]
      it.remove();
    }
  }
}
