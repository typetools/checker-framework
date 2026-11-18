// Tests a delegator that implements an interface but declares its delegate field using a concrete
// implementation type.  The delegate's class need not be a supertype of the delegator.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.common.delegation.qual.*;

public abstract class InterfaceDelegatorTest<E> implements List<E> {

  @Delegate private ArrayList<E> list;

  InterfaceDelegatorTest(ArrayList<E> list) {
    this.list = list;
  }

  @Override
  public int size() {
    return list.size(); // OK
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty(); // OK
  }

  @Override
  public boolean add(E e) {
    return list.add(e); // OK
  }

  @Override
  public E get(int index) {
    return list.get(index); // OK
  }

  @Override
  // :: warning: [invalid.delegate]
  public void clear() {
    // trimToSize() is unrelated to clear(), even though it is called on the delegate.
    list.trimToSize();
  }
}
