// Tests the @DelegatorMustOverride annotation.

import org.checkerframework.common.delegation.qual.*;

class MustOverrideSuper {

  @DelegatorMustOverride
  public boolean isEmpty() {
    return true;
  }

  // A delegator need not override this method.
  public int size() {
    return 0;
  }
}

class GoodDelegator extends MustOverrideSuper {

  @Delegate private MustOverrideSuper delegate;

  GoodDelegator(MustOverrideSuper delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty(); // OK
  }
}

// :: warning: [delegate.override]
class BadDelegator extends MustOverrideSuper {

  @Delegate private MustOverrideSuper delegate;

  BadDelegator(MustOverrideSuper delegate) {
    this.delegate = delegate;
  }
}

// A class with no @Delegate field is not a delegator, so it is not checked.
class NotADelegator extends MustOverrideSuper {}
