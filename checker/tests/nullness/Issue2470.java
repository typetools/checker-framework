import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public class Issue2470 {
  static class Example {
    @MonotonicNonNull String s;

    public Example() {}

    @EnsuresNonNull("this.s")
    public Example setS(String s1) {
      this.s = s1;
      return this;
    }

    // TODO: Support "return" in Java Expression syntax.
    // @EnsuresNonNull("return.s")
    @EnsuresNonNull("this.s")
    public Example setS2(String s1) {
      this.s = s1;
      return this;
    }

    @RequiresNonNull("this.s")
    public void print() {
      System.out.println(this.s.toString());
    }
  }

  static void buggy() {
    new Example()
        // :: error: (contracts.precondition)
        .print();
  }

  static void ok() {
    Example e = new Example();
    e.setS("test");
    e.print();
  }

  static void buggy2() {
    new Example()
        .setS("test")
        // :: error:(contracts.precondition)
        .print();
  }

  // TODO: These should be legal, once "return" is supported in Java Expression syntax.
  // of a method.
  /*
  static void ok3() {
      Example e = new Example().setS2("test");
      e.print();
  }

  static void ok2() {
      new Example().setS2("test").print();
  }
  */
}
