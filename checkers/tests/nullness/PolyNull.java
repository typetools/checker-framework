import checkers.nullness.quals.*;

class Test {
   @PolyNull String identity(@PolyNull String str) { return str; }
   void test1() { identity(null); }
   void test2() { identity((/*@Nullable*/ String) null); }
}
