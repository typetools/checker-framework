// @skip-test temporary

import org.checkerframework.checker.calledmethods.qual.CalledMethods;

public class ECMInference {

  class A1 {
    Object field;

    void doStuff() {
      field.toString();
    }

    void client() {
      doStuff();
      @CalledMethods("toString") A1 a1 = this;
    }
  }

  class B1 extends A1 {
    void doStuff() {
      field.toString();
    }

    void client() {
      doStuff();
      @CalledMethods("toString") B1 b1 = this;
    }
  }

  class A2 {
    Object field;

    void doStuff() {
      field.toString();
    }

    void client(@CalledMethods("toString") A2 this) {
      doStuff();
      @CalledMethods("toString") A2 a2 = this;
    }
  }

  class B2 extends A2 {
    void doStuff() {
      field.toString();
      field.hashCode();
    }

    void client() {
      doStuff();
      @CalledMethods({"toString", "hashCode"}) B2 b2 = this;
    }
  }
}
