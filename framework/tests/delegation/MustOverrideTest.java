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

// A method and a method that overrides it may both be annotated with @DelegatorMustOverride.  A
// delegator that overrides neither is warned about the method only once.
interface DuplicateSuperInterface {
  @DelegatorMustOverride
  boolean isEmpty();
}

class DuplicateSuperClass implements DuplicateSuperInterface {
  @Override
  @DelegatorMustOverride
  public boolean isEmpty() {
    return true;
  }
}

// :: warning: [delegate.override]
class DuplicateDelegator extends DuplicateSuperClass {

  @Delegate private DuplicateSuperClass delegate;

  DuplicateDelegator(DuplicateSuperClass delegate) {
    this.delegate = delegate;
  }
}

// @DelegatorMustOverride may not be written on a method that no subclass can override.
class NotOverridable {

  @DelegatorMustOverride
  // :: error: [mustoverride.not.overridable]
  public static boolean staticMethod() {
    return true;
  }

  @DelegatorMustOverride
  // :: error: [mustoverride.not.overridable]
  public final boolean finalMethod() {
    return true;
  }

  @DelegatorMustOverride
  // :: error: [mustoverride.not.overridable]
  private boolean privateMethod() {
    return true;
  }
}

// No delegate.override warning: none of the methods above can be overridden.
class NotOverridableDelegator extends NotOverridable {

  @Delegate private NotOverridable delegate;

  NotOverridableDelegator(NotOverridable delegate) {
    this.delegate = delegate;
  }
}
