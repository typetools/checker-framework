import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.calledmethodsonelements.qual.*;
import org.checkerframework.checker.calledmethodsonelements.qual.EnsuresCalledMethodsOnElements;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.mustcallonelements.qual.*;

@InheritableMustCall({"close", "foo"})
class Resource {
  public void foo() {}

  public void close() {}
}

class illegalOwningArrayField {
  // non-final owningarray field is illegal
  // :: error: owningarray.field.not.final
  @OwningArray Resource[] arr;
  // static owningarray field is illegal
  // :: error: owningarray.field.static
  // :: error: unfulfilled.mustcallonelements.obligations
  static final @OwningArray Resource[] arr2 = new Resource[1];
}

@InheritableMustCall("close")
class multipleOwningArrayFieldAssignment {
  final @OwningArray Resource[] arr;

  @EnsuresCalledMethodsOnElements(
      value = "arr",
      methods = {"close", "foo"})
  public void close() {
    // :: error: owningarray.field.assigned.outside.constructor
    arr[0] = null;
    for (int i = 0; i < arr.length; i++) {
      arr[i].close();
    }
    for (int i = 0; i < arr.length; i++) {
      arr[i].foo();
    }
  }

  public multipleOwningArrayFieldAssignment(int n) {
    arr = new Resource[n];
    // illegal assignment - only in pattern-matched loop allowed
    // :: error: illegal.owningarray.field.elements.assignment
    arr[0] = null;
    for (int i = 0; i < n; i++) {
      arr[i] = new Resource();
    }
    for (int i = 0; i < n; i++) {
      arr[i].close();
    }
    for (int i = 0; i < n; i++) {
      arr[i].foo();
    }
    for (int i = 0; i < n; i++) {
      // :: error: owningarray.field.elements.assigned.multiple.times
      arr[i] = new Resource();
    }
  }
}

class NoDestructorMethodForOwningArrayField {
  // no mustcall annotation on class (for destructor method)
  // :: error: unfulfilled.mustcallonelements.obligations
  final @OwningArray Resource[] arr = new Resource[10];
}

@InheritableMustCall("close")
class DestructorMethodWithoutEnsuresCmoeForOwningArrayField {
  // mustcall annotation on class doesn't have a EnsuresCmoe annotation
  // :: error: unfulfilled.mustcallonelements.obligations
  final @OwningArray Resource[] arr = new Resource[10];

  public void close() {}
}

@InheritableMustCall("close")
class DestructorMethodWithInsufficientEnsuresCmoeForOwningArrayField {
  // destructor method doesn't cover all calling obligations
  // :: error: unfulfilled.mustcallonelements.obligations
  final @OwningArray Resource[] arr = new Resource[10];

  @EnsuresCalledMethodsOnElements(value = "arr", methods = "close")
  public void close() {
    for (int i = 0; i < arr.length; i++) {
      arr[i].close();
    }
  }
}

@InheritableMustCall("close")
class DestructorMethodWithInvalidEnsuresCmoeForOwningArrayField {
  final @OwningArray Resource[] arr = new Resource[10];

  @EnsuresCalledMethodsOnElements(
      value = "arr",
      methods = {"foo", "close"})
  // destructor method doesn't fulfill post-condition
  // :: error: contracts.postcondition
  public void close() {}
}

@InheritableMustCall({"destruct", "close"})
class ValidOwningArrayField {
  final @OwningArray Resource[] arr;

  public Resource[] getField() {
    // :: error: return.owningarray
    return arr;
  }

  public ValidOwningArrayField(@OwningArray Resource[] arr) {
    this.arr = arr;
  }

  public ValidOwningArrayField(Resource[] arr, int k) {
    // k is here simply because without it, we would have a duplicate constructor.
    // assignment is illegal since the arr parameter is not @OwningArray
    // :: error: illegal.owningarray.field.assignment
    this.arr = arr;
  }

  @EnsuresCalledMethodsOnElements(
      value = "arr",
      methods = {"close"})
  public void close() {
    for (int i = 0; i < arr.length; i++) {
      arr[i].close();
    }
  }

  @EnsuresCalledMethodsOnElements(
      value = "arr",
      methods = {"foo"})
  public void destruct() {
    for (int i = 0; i < arr.length; i++) {
      arr[i].foo();
    }
  }
}

class EvilOwningArrayWrapperClient {
  public EvilOwningArrayWrapperClient() {
    int n = 10;
    @OwningArray Resource[] localarr = new Resource[n];
    for (int i = 0; i < n; i++) {
      localarr[i] = new Resource();
    }
    // give up ownership to constructor
    ValidOwningArrayField d = new ValidOwningArrayField(localarr);
    // this loop doesn't pattern-match, simply because d.arr is not accepted
    // as an identifier (it is a MemberSelectTree). A warning is issued to inform
    // the programmer that d.arr is an unexpected array expression.
    for (int i = 0; i < d.arr.length; i++) {
      // :: error: required.method.not.called
      // :: error: illegal.owningarray.field.elements.assignment
      // :: warning: unexpected.array.expression
      d.arr[i] = new Resource();
    }
    // fulfill the obligations of 'd'
    d.destruct();
    d.close();
  }

  public void tryCapturingOwningArrayField() {
    int n = 10;
    @OwningArray Resource[] localarr = new Resource[n];
    for (int i = 0; i < n; i++) {
      localarr[i] = new Resource();
    }
    // give up ownership to constructor
    ValidOwningArrayField d = new ValidOwningArrayField(localarr);
    // try reassigning the elements of array, despite lost ownership
    for (int i = 0; i < n; i++) {
      // :: error: assignment.without.ownership
      localarr[i] = new Resource();
    }
    // this method call is not allowed either, due to the missing ownership over localarr
    // :: error: argument
    // :: error: argument.with.revoked.ownership
    methodWithOwningArrayParameter(localarr);

    // reassign localarr to a new array, which is legal.
    // However, after its elements have been assigned, they
    // are never closed, which throws an error.
    // :: error: unfulfilled.mustcallonelements.obligations
    localarr = new Resource[n];
    // assigning its elements is now allowed
    for (int i = 0; i < n; i++) {
      localarr[i] = new Resource();
    }

    // try to capture the @OwningArray field of 'd'
    // since the RHS of the assignment is @OwningArray, that is illegal aliasing.
    // :: error: illegal.aliasing
    Resource[] capture = d.arr;
    // this illegal assignment counts as a field assignment
    // and since the field is @OwningArray, it is forbidden
    // :: error: owningarray.field.assigned.outside.constructor
    d.arr[0] = null;

    // fulfill the obligations of 'd'
    d.destruct();
    d.close();
  }

  private void methodWithOwningArrayParameter(@OwningArray Resource[] arr) {
    for (int i = 0; i < arr.length; i++) {
      arr[i].close();
    }
    for (int i = 0; i < arr.length; i++) {
      arr[i].foo();
    }
  }
}

class OwningArrayFieldClient {
  public void m() {
    int n = 10;
    @OwningArray Resource[] localarr = new Resource[n];
    for (int i = 0; i < n; i++) {
      localarr[i] = new Resource();
    }
    // d.close() would also have to be called
    // :: error: required.method.not.called
    ValidOwningArrayField d = new ValidOwningArrayField(localarr);
    d.destruct();
  }
}
