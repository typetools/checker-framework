// Tests that a delegator must override each concrete method that it would otherwise inherit, unless
// the method is annotated with @InheritableByDelegator.

import org.checkerframework.common.delegation.qual.*;

class Counter {
  private int count;

  public int count() {
    return count;
  }

  public void increment() {
    count++;
  }

  @InheritableByDelegator
  public boolean isZero() {
    return count() == 0;
  }

  public final int finalMethod() {
    return count;
  }

  public static int staticMethod() {
    return 0;
  }
}

// :: warning: [delegator.must.override]
class IncompleteCounterDelegator extends Counter {

  @Delegate private Counter delegate;

  @Override
  public int count() {
    return delegate.count();
  }

  // increment() is not overridden, so the inherited implementation would increment the
  // delegator's own count field.
}

class CompleteCounterDelegator extends Counter {

  @Delegate private Counter delegate;

  @Override
  public int count() {
    return delegate.count();
  }

  @Override
  public void increment() {
    delegate.increment();
  }

  // No warning about isZero(), which is annotated with @InheritableByDelegator; about
  // finalMethod() or staticMethod(), which cannot be overridden; or about the methods of Object.
}

// A delegator that extends another delegator inherits methods that forward to the other
// delegator's delegate.
// :: warning: [delegator.must.override]
class SubCounterDelegator extends CompleteCounterDelegator {

  @Delegate private Counter otherDelegate;

  @Override
  public int count() {
    return otherDelegate.count();
  }
}

/** A class that is not a delegator, so it is not checked. */
class NotADelegator extends Counter {}

interface Shape {
  double area();

  default boolean hasZeroArea() {
    return area() == 0;
  }
}

class ShapeDelegator implements Shape {

  @Delegate private Shape delegate;

  @Override
  public double area() {
    return delegate.area();
  }

  // No warning about hasZeroArea(), which is a default method.
}
