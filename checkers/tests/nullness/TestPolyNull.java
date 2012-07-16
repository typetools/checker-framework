import checkers.nullness.quals.*;

class TestPolyNull {
   @PolyNull String identity(@PolyNull String str) { return str; }
   void test1() { identity(null); }
   void test2() { identity((@Nullable String) null); }

   public static @PolyNull String[] typeArray(@PolyNull Object[] seq) {
    @PolyNull String[] retval = new @PolyNull String[seq.length];
    for (int i = 0 ; i < seq.length ; i++) {
      if (seq[i] == null) {
        // null can be assigned into the PolyNull array, because we
        // performed a test on seq and know that it is nullable.
        retval[i] = null;
        // One can always add a dummy value: nonnull is the bottom
        // type and legal for any instantiation of PolyNull.
        retval[i] = "dummy";
      } else {
        retval[i] = seq[i].getClass().toString();
      }
    }
    return retval;
  }

}
