// Tests a delegator whose delegate's class inherits an implementation of an interface method from
// a superclass that does not implement the interface.

import org.checkerframework.common.delegation.qual.*;

interface InheritedImplI {
  int m();

  int n();
}

class InheritedImplBase {
  public int m() {
    return 1;
  }

  public int unrelated() {
    return 2;
  }
}

class InheritedImplImpl extends InheritedImplBase implements InheritedImplI {
  @Override
  public int n() {
    return 3;
  }
}

class InheritedImplementationTest implements InheritedImplI {

  @Delegate private InheritedImplImpl delegate;

  InheritedImplementationTest(InheritedImplImpl delegate) {
    this.delegate = delegate;
  }

  @Override
  public int m() {
    // The call resolves to InheritedImplBase.m(), which implements InheritedImplI.m() in
    // InheritedImplImpl.
    return delegate.m(); // OK
  }

  @Override
  // :: warning: [invalid.delegate]
  public int n() {
    return delegate.unrelated();
  }
}
