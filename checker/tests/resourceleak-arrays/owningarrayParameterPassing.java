import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.calledmethodsonelements.qual.*;
import org.checkerframework.checker.calledmethodsonelements.qual.EnsuresCalledMethodsOnElements;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.mustcallonelements.qual.*;

class OwningarrayParameterPassing {
  private final int n = 10;

  // :: error: unfulfilled.mustcallonelements.obligations
  private void owningParameter(@OwningArray Resource[] param) {
    // takes ownership to call all methods (foo, close)
  }

  private void owningParameterWithPartialObligations(
      // :: error: unfulfilled.mustcallonelements.obligations
      @OwningArray Resource @MustCallOnElements("close") [] param) {
    // takes ownership only of "close"
  }

  private void nonOwningParameter(Resource[] param) {
    // takes no ownership whatsoever
  }

  public void passNonOwningToOwning() {
    Resource[] nonOwningArr = new Resource[n];
    // non-owning to owning is illegal
    // :: error: unexpected.argument.ownership
    owningParameter(nonOwningArr);
  }

  public void passNonOwningToNonOwning() {
    Resource[] nonOwningArr = new Resource[n];
    // non-owning to non-owning is legal
    nonOwningParameter(nonOwningArr);
  }

  public void passOwningToNonOwning() {
    // :: error: unfulfilled.mustcallonelements.obligations
    @OwningArray Resource[] owningArr = new Resource[n];
    for (int i = 0; i < n; i++) {
      owningArr[i] = new Resource();
    }
    // owning to non-owning is illegal, since the non-owning parameter would be an alias
    // :: error: unexpected.argument.ownership
    nonOwningParameter(owningArr);
  }

  public void passOwningToOwning() {
    @OwningArray Resource[] owningArr = new Resource[n];
    for (int i = 0; i < n; i++) {
      owningArr[i] = new Resource();
    }
    // is legal and resolved the calling obligations of 'owningArr'
    owningParameter(owningArr);
  }

  public void passOwningToPartialOwning() {
    @OwningArray Resource[] owningArr = new Resource[n];
    for (int i = 0; i < n; i++) {
      owningArr[i] = new Resource();
    }
    // method expects mcoe("close"), but it mcoe("foo", "close")
    // :: error: argument
    owningParameterWithPartialObligations(owningArr);
  }

  public void partiallyFulfillUsingMethod() {
    @OwningArray Resource[] owningArr = new Resource[n];
    for (int i = 0; i < n; i++) {
      owningArr[i] = new Resource();
    }
    for (int i = 0; i < n; i++) {
      owningArr[i].foo();
    }
    // method expects mcoe("close"), which it is.
    // fulfills the remaining obligation for us.
    owningParameterWithPartialObligations(owningArr);
  }

  public void revokeOwnershipAndRegain() {
    @OwningArray Resource[] owningArr = new Resource[n];
    for (int i = 0; i < n; i++) {
      owningArr[i] = new Resource();
    }
    // passing owningArr as @OwningArray argument to a constructor revokes ownership.
    // none of the methods should be able to be called after that.
    OwningArrayWrapper wrapper = new OwningArrayWrapper(owningArr);
    // :: error: argument
    // :: error: argument.with.revoked.ownership
    nonOwningParameter(owningArr);
    // :: error: argument
    // :: error: argument.with.revoked.ownership
    owningParameter(owningArr);
    // :: error: argument
    // :: error: argument.with.revoked.ownership
    owningParameterWithPartialObligations(owningArr);
    // reallocate owningArr, giving it back ownership over the new array
    owningArr = new Resource[n];
    for (int i = 0; i < n; i++) {
      owningArr[i] = new Resource();
    }
    // this method call now fails due to the different ownership annotations
    // :: error: unexpected.argument.ownership
    nonOwningParameter(owningArr);
    // this method call now succeeds and clears the obligations of the new array
    owningParameter(owningArr);
    // fulfill the obligation of 'wrapper'
    wrapper.close();
  }
}

@InheritableMustCall("close")
class OwningArrayWrapper {
  final @OwningArray Resource[] arr;

  public OwningArrayWrapper(@OwningArray Resource[] arr) {
    this.arr = arr;
  }

  @EnsuresCalledMethodsOnElements(
      value = "arr",
      methods = {"foo", "close"})
  public void close() {
    for (int i = 0; i < arr.length; i++) {
      arr[i].foo();
    }
    for (int i = 0; i < arr.length; i++) {
      arr[i].close();
    }
  }
}
