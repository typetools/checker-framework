import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

// @skip-test https://github.com/typetools/checker-framework/issues/3400
public class SubClassHasQP {
  @HasQualifierParameter(Tainted.class)
  static class Buffer {
    void append(@PolyTainted Buffer this, @PolyTainted String s) {}

    void append2(@PolyTainted Buffer this, @PolyTainted String s) {}
  }

  @HasQualifierParameter(Tainted.class)
  static @Untainted class UntaintedBuffer extends @Untainted Buffer {
    @Override
    // :: error: (annotations.on.use)
    void append(@Tainted UntaintedBuffer this, @Tainted String s) {}

    @Override
    void append2(@Untainted UntaintedBuffer this, @Untainted String s) {}
  }

  @HasQualifierParameter(Tainted.class)
  static @Tainted class TaintedBuffer extends @Tainted Buffer {
    @Override
    void append(@Tainted TaintedBuffer this, @Tainted String s) {} // legal override

    @Override
    void append2(@Untainted TaintedBuffer this, String s) {
      @Untainted Buffer that = this;
    }
  }

  @HasQualifierParameter(Tainted.class)
  // :: error: (super.invocation)
  static class MyTaintedBuffer extends TaintedBuffer {
    @Override
    // :: error: (override.receiver)
    void append(MyTaintedBuffer this, String s) {} // legal override
  }

  @HasQualifierParameter(Tainted.class)
  @Tainted class MyTaintedBuffer2 extends TaintedBuffer {
    @Override
    void append(@Tainted MyTaintedBuffer2 this, String s) {} // legal override
  }
}
