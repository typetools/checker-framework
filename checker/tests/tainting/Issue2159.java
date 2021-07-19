import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Issue2159 {
  @Tainted Issue2159() {}

  static class MyClass extends Issue2159 {
    MyClass() {}

    // :: error: (super.invocation.invalid)
    @PolyTainted MyClass(@PolyTainted Object x) {}

    void testPolyTaintedLocal(
        @PolyTainted Object input, @Untainted Object untainted, @Tainted Object tainted) {
      // :: warning: (cast.unsafe)
      @PolyTainted Object local = (@PolyTainted MyClass) new MyClass();
      // :: warning: (cast.unsafe.constructor.invocation)
      @PolyTainted Object local1 = new @PolyTainted MyClass();
      // :: warning: (cast.unsafe.constructor.invocation)
      @Untainted Object local2 = new @Untainted MyClass();

      @PolyTainted Object local3 = new @PolyTainted MyClass(input);
      // :: warning: (cast.unsafe.constructor.invocation)
      @Untainted Object local4 = new @Untainted MyClass(input);
      // :: warning: (cast.unsafe.constructor.invocation)
      @PolyTainted Object local5 = new @PolyTainted MyClass(tainted);
      @Untainted Object local6 = new @Untainted MyClass(untainted);
    }
  }
}
