import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.calledmethodsonelements.qual.*;
import org.checkerframework.checker.calledmethodsonelements.qual.EnsuresCalledMethodsOnElements;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.mustcallonelements.qual.*;
import org.checkerframework.framework.qual.*;

@InheritableMustCall({"close", "foo"})
class Resource {
  public void foo() {}

  public void close() {}
}

@InheritableMustCall({"destruct", "close"})
class OwningArrayField {
  final @OwningArray Resource[] arr;

  public OwningArrayField(@OwningArray Resource[] arr) {
    this.arr = arr;
  }

  public OwningArrayField(Resource[] arr, int k) {
    // k is here simply because without it, we would have a duplicate constructor.
    // it's illegal since the arr parameter is not @OwningArray
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

class OwningArrayFieldClient {
  public void m() {
    int n = 10;
    @OwningArray Resource[] localarr = new Resource[n];
    for (int i = 0; i < n; i++) {
      localarr[i] = new Resource();
    }
    // d.close() would also have to be called
    // :: error: required.method.not.called
    OwningArrayField d = new OwningArrayField(localarr);
    d.destruct();
  }
}
