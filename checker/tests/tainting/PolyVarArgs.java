import org.checkerframework.checker.tainting.qual.*;

class PolyVarArgs {

  void testVarArgsNoFormals() {
    @Tainted String tainted = varArgsNoFormals();
    @Untainted String untainted = varArgsNoFormals("a");
    @Untainted String untainted2 = varArgsNoFormals("b", "c");
  }

  void testVarArgsNoFormalsInvalid() {
    // :: error: (assignment)
    @Untainted String tainted = varArgsNoFormals();
  }

  void testVarArgsWithFormals() {
    @Tainted String tainted = varArgsWithFormals(1);
    @Untainted String untainted = varArgsWithFormals(1, "a");
    @Untainted String untainted2 = varArgsWithFormals(1, "a", "b");
  }

  void testVarArgsWithFormalsInvalid() {
    // :: error: (assignment)
    @Untainted String tainted = varArgsWithFormals(1);
  }


  @PolyTainted String varArgsNoFormals(@PolyTainted String... s) {
    throw new Error();
  }

  @PolyTainted String varArgsWithFormals(int a, @PolyTainted String... s) {
    throw new Error();
  }
}

