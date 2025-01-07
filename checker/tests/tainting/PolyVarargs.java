import org.checkerframework.checker.tainting.qual.*;

class PolyVarargs {

  void testVarargsNoFormals() {
    @Tainted String tainted = varArgsNoFormals();
    @Untainted String untainted = varArgsNoFormals("a");
    @Untainted String untainted2 = varArgsNoFormals("b", "c");
  }

  void testVarargsNoFormalsInvalid() {
    // :: error: (assignment)
    @Untainted String tainted = varArgsNoFormals();
  }

  void testVarargsWithFormals() {
    @Tainted String tainted = varArgsWithFormals(1);
    @Untainted String untainted = varArgsWithFormals(1, "a");
    @Untainted String untainted2 = varArgsWithFormals(1, "a", "b");
  }

  void testVarargsWithFormalsInvalid() {
    // :: error: (assignment)
    @Untainted String tainted = varArgsWithFormals(1);
  }

  void testVarargsWithPolyFormals() {
    @Tainted String tainted = varArgsWithPolyFormals(1);

    // :: warning: (cast.unsafe)
    @Untainted int safeInt = (@Untainted int) 1;
    @Untainted String untainted = varArgsWithPolyFormals(safeInt, "a");
  }

  void testVarargsWithPolyFormalsInvalid() {
    // :: error: (assignment)
    @Untainted String tainted = varArgsWithPolyFormals(1);
  }

  @PolyTainted String varArgsNoFormals(@PolyTainted String... s) {
    throw new Error();
  }

  @PolyTainted String varArgsWithFormals(int a, @PolyTainted String... s) {
    throw new Error();
  }

  @PolyTainted String varArgsWithPolyFormals(@PolyTainted int a, @PolyTainted String... s) {
    throw new Error();
  }
}
