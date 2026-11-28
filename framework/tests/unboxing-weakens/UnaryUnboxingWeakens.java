import org.checkerframework.framework.testchecker.unboxingweakens.qual.UWStrong;

// The operand of a unary numeric or logical operator is unboxed, which changes @UWStrong to @UWTop.
public class UnaryUnboxingWeakens {
  void unaryPlus(@UWStrong Integer i) {
    // :: error: (assignment)
    @UWStrong int result = +i;
  }

  void unaryMinus(@UWStrong Integer i) {
    // :: error: (assignment)
    @UWStrong int result = -i;
  }

  void bitwiseComplement(@UWStrong Integer i) {
    // :: error: (assignment)
    @UWStrong int result = ~i;
  }

  void logicalComplement(@UWStrong Boolean b) {
    // :: error: (assignment)
    @UWStrong boolean result = !b;
  }

  void noUnboxing(@UWStrong int i, @UWStrong boolean b) {
    @UWStrong int r1 = +i;
    @UWStrong int r2 = -i;
    @UWStrong int r3 = ~i;
    @UWStrong boolean r4 = !b;
  }
}
