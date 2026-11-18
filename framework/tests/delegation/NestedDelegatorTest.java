// Tests that the delegate field of an enclosing class is not used for a nested class, and vice
// versa.

import java.util.ArrayList;
import org.checkerframework.common.delegation.qual.*;

public class NestedDelegatorTest<E> extends ArrayList<E> {

  @Delegate private ArrayList<E> outerDelegate;

  NestedDelegatorTest(ArrayList<E> outerDelegate) {
    this.outerDelegate = outerDelegate;
  }

  @Override
  public void clear() {
    outerDelegate.clear(); // OK
  }

  /** A nested class that is itself a delegator, to a different field. */
  class Inner extends ArrayList<E> {

    @Delegate private ArrayList<E> innerDelegate;

    Inner(ArrayList<E> innerDelegate) {
      this.innerDelegate = innerDelegate;
    }

    @Override
    public void clear() {
      innerDelegate.clear(); // OK
    }

    @Override
    // :: warning: [invalid.delegate]
    public boolean isEmpty() {
      return outerDelegate.isEmpty();
    }
  }

  /** A nested class that is not a delegator, so it is not checked. */
  class NotADelegator extends ArrayList<E> {
    @Override
    public void clear() {
      // No warning, even though this class does not delegate.
    }
  }

  @Override
  public boolean isEmpty() {
    return outerDelegate.isEmpty(); // OK: the enclosing class's delegate is used again
  }
}
