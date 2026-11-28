// `Deque.push` takes ownership of its argument.  A collection that cannot hold an obligation
// cannot take ownership, so the pushed resource's obligation remains.

import java.util.Deque;
import java.util.List;
import java.util.Stack;

class OwningInsertIntoBottomCollection {

  void push(List<Deque<Resource>> deques) {
    for (Deque<Resource> d : deques) {
      // :: error: required.method.not.called
      d.push(new Resource());
    }
  }

  void add(List<Deque<Resource>> deques) {
    for (Deque<Resource> d : deques) {
      // :: error: required.method.not.called
      d.add(new Resource());
    }
  }

  void otherDequeInserts(List<Deque<Resource>> deques) {
    for (Deque<Resource> d : deques) {
      // :: error: required.method.not.called
      d.offer(new Resource());
      // :: error: required.method.not.called
      d.offerFirst(new Resource());
      // :: error: required.method.not.called
      d.offerLast(new Resource());
      // :: error: required.method.not.called
      d.addFirst(new Resource());
      // :: error: required.method.not.called
      d.addLast(new Resource());
    }
  }

  void stackPush(List<Stack<Resource>> stacks) {
    for (Stack<Resource> s : stacks) {
      // :: error: required.method.not.called
      s.push(new Resource());
    }
  }
}
