// Test: Field is initialized in a constructor that calls super(), not this().
// Expected: Warning in only open(), not in the constructor.

import java.io.FileInputStream;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"close"})
class SuperConstructorLeak extends Parent {
  private @Owning FileInputStream s;

  public SuperConstructorLeak() throws Exception {
    super(); // Explicit call to superclass constructor
    s = new FileInputStream("test.txt");
  }

  // :: error: (missing.creates.mustcall.for)
  public void open() {
    try {
      // :: error: (required.method.not.called)
      s = new FileInputStream("test.txt");
    } catch (Exception e) {
    }
  }

  @EnsuresCalledMethods(value = "this.s", methods = "close")
  public void close() {
    try {
      s.close();
    } catch (Exception e) {
    }
  }
}

class Parent {
  public Parent() {
    System.out.println("parent constructor");
  }
}
