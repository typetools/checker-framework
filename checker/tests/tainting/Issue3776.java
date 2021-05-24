import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Issue3776 {
  class MyInnerClass {
    public MyInnerClass() {}

    public MyInnerClass(@Untainted String s) {}

    public MyInnerClass(int... i) {}
  }

  static class MyClass {
    public MyClass() {}

    public MyClass(@Untainted String s) {}

    public MyClass(int... i) {}
  }

  void test(Issue3776 outer, @Tainted String tainted) {
    new MyInnerClass("1") {};
    this.new MyInnerClass("2") {};
    new MyClass() {};
    // :: error: (argument)
    new MyClass(tainted) {};
    new MyClass(1, 2, 3) {};
    new MyClass(1) {};
    new MyInnerClass() {};
    // :: error: (argument)
    new MyInnerClass(tainted) {};
    new MyInnerClass(1) {};
    new MyInnerClass(1, 2, 3) {};
    this.new MyInnerClass() {};
    // :: error: (argument)
    this.new MyInnerClass(tainted) {};
    this.new MyInnerClass(1) {};
    this.new MyInnerClass(1, 2, 3) {};
    outer.new MyInnerClass() {};
    // :: error: (argument)
    outer.new MyInnerClass(tainted) {};
    outer.new MyInnerClass(1) {};
    outer.new MyInnerClass(1, 2, 3) {};
  }
}
