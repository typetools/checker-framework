import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

public class Casts {
  @HasQualifierParameter(Tainted.class)
  static class Buffer {}

  @HasQualifierParameter(Tainted.class)
  static class MyBuffer extends Buffer {}

  void test(
      @Tainted Buffer taintedBuf,
      @Untainted Buffer untaintedBuf,
      @Tainted Object taintedObj,
      @Untainted Object untaintedObj) {
    @Tainted Object o = (@Tainted Object) taintedBuf;
    o = (@Tainted Object) untaintedObj;
    o = (@Tainted Object) untaintedBuf;

    // :: error: (invariant.cast.unsafe)
    o = (@Tainted Buffer) taintedObj;
    // :: error: (invariant.cast.unsafe)
    o = (@Tainted Buffer) untaintedBuf;
    // :: error: (invariant.cast.unsafe)
    o = (@Tainted Buffer) untaintedObj;

    // :: warning: (cast.unsafe)
    o = (@Untainted Object) taintedObj;
    // :: warning: (cast.unsafe)
    o = (@Untainted Object) taintedBuf;
    o = (@Untainted Object) untaintedBuf;

    // :: error: (invariant.cast.unsafe)
    o = (@Untainted Buffer) taintedObj;
    // :: error: (invariant.cast.unsafe)
    o = (@Untainted Buffer) taintedBuf;
    o = (@Untainted Buffer) untaintedObj;
  }

  void test2(
      @Tainted Buffer taintedBuf,
      @Untainted Buffer untaintedBuf,
      @Tainted MyBuffer taintedMyBuf,
      @Untainted MyBuffer untaintedMyBuff) {
    @Tainted Object o = (@Tainted Buffer) taintedMyBuf;
    // :: error: (invariant.cast.unsafe)
    o = (@Tainted Buffer) untaintedBuf;
    // :: error: (invariant.cast.unsafe)
    o = (@Tainted Buffer) untaintedMyBuff;

    o = (@Tainted MyBuffer) taintedBuf;
    // :: error: (invariant.cast.unsafe)
    o = (@Tainted MyBuffer) untaintedBuf;
    // :: error: (invariant.cast.unsafe)
    o = (@Tainted MyBuffer) untaintedMyBuff;

    // :: error: (invariant.cast.unsafe)
    o = (@Untainted Buffer) taintedMyBuf;
    // :: error: (invariant.cast.unsafe)
    o = (@Untainted Buffer) taintedMyBuf;
    o = (@Untainted Buffer) untaintedMyBuff;

    // :: error: (invariant.cast.unsafe)
    o = (@Untainted MyBuffer) taintedBuf;
    // :: error: (invariant.cast.unsafe)
    o = (@Untainted MyBuffer) taintedMyBuf;
    o = (@Untainted MyBuffer) untaintedMyBuff;
  }
}
