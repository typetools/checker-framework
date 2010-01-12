import checkers.nullness.quals.*;

class Test {
   @PolyNull String identity(@PolyNull String str) { return str; }
   void test1() { identity(null); }
   void test2() { identity((/*@Nullable*/ String) null); }

   public static /*@PolyNull*/ String[] typeArray(/*@PolyNull*/ Object[] seq) {
    /*@PolyNull*/ String[] retval = new /*@PolyNull*/ String[seq.length];
    for (int i = 0 ; i < seq.length ; i++) {
      if (seq[i] == null) {
        retval[i] = null;
      } else {
        retval[i] = seq[i].getClass().toString();
      }
    }
    return retval;
  }

}
