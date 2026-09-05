// Tests that the receiver of a delegate call must be the delegate field itself, not merely a
// field that has the same name.

import java.util.ArrayList;
import org.checkerframework.common.delegation.qual.*;

public class DelegateReceiverTest<E> extends ArrayList<E> {

  @Delegate private ArrayList<E> list;

  private Holder<E> holder;

  DelegateReceiverTest(ArrayList<E> list, Holder<E> holder) {
    this.list = list;
    this.holder = holder;
  }

  @Override
  public void clear() {
    list.clear(); // OK
  }

  @Override
  // :: warning: [invalid.delegate]
  public boolean isEmpty() {
    // The receiver is another object's field that happens to have the same name.
    return holder.list.isEmpty();
  }

  @Override
  // :: warning: [invalid.delegate]
  public int size() {
    // The receiver is a static field that happens to have the same name.
    return StaticHolder.list.size();
  }
}

class Holder<E> {
  ArrayList<E> list = new ArrayList<>();
}

class StaticHolder {
  static ArrayList<Object> list = new ArrayList<>();
}
