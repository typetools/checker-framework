import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.MaybeReplaceable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Replaceable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;

/**
 * A {@code Queue} has no replace methods, so {@code @Modifiable} makes no claim about replacing
 * into it -- unless it is also a {@code List}, which does have replace methods.
 */
public class ListQueueTest {

  /** A list that is also a queue, like {@code LinkedList}. */
  static class ListQueue<E> extends ArrayList<E> implements Queue<E> {
    @Override
    public boolean offer(@Growable ListQueue<E> this, E e) {
      return add(e);
    }

    @Override
    public E remove(@Shrinkable ListQueue<E> this) {
      return remove(0);
    }

    @Override
    public E poll(@Shrinkable ListQueue<E> this) {
      return isEmpty() ? null : remove(0);
    }

    @Override
    public E element() {
      return get(0);
    }

    @Override
    public E peek() {
      return isEmpty() ? null : get(0);
    }
  }

  void userListQueue(@Modifiable ListQueue<String> q) {
    @Replaceable ListQueue<String> replaceable = q;
  }

  void linkedList(@Modifiable LinkedList<String> q) {
    @Replaceable LinkedList<String> replaceable = q;
  }

  void plainQueue(@Modifiable Queue<String> q) {
    @MaybeReplaceable Queue<String> maybe = q;
    // :: error: [assignment]
    @Replaceable Queue<String> replaceable = q;
  }
}
