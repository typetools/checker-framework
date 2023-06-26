// @skip-test if -AenableWPIForRLC flag is not passed
import org.checkerframework.checker.calledmethods.qual.CalledMethods;

public class ECMInference {

  class A1 {
    void doStuff() {
      toString();
    }

    void clientA1() {
      doStuff();
      // :: warning: (assignment)
      @CalledMethods("toString") A1 a1 = this;
    }
  }

  class B1 extends A1 {
    @Override
    void doStuff() {
      toString();
    }

    void clientB1() {
      doStuff();
      // :: warning: (assignment)
      @CalledMethods("toString") B1 b1 = this;
    }
  }

  class A2 {
    void doStuff() {
      toString();
    }

    void clientA2() {
      doStuff();
      // :: warning: (assignment)
      @CalledMethods("toString") A2 a2 = this;
    }
  }

  class B2 extends A2 {
    @Override
    void doStuff() {
      toString();
      hashCode();
    }

    void clientB2() {
      doStuff();
      // :: warning: (assignment)
      @CalledMethods({"toString", "hashCode"}) B2 b2 = this;
    }
  }
}
