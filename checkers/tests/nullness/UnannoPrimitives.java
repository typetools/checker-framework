import checkers.nullness.quals.*;

class UnannoPrimitives {
  //:: error: (type.invalid)
  @Nullable int f;

  //:: error: (type.invalid)
  @NonNull int g;

  void local() {
    // test whether an arbitrary declaration annotation gets confused
    @SuppressWarnings("tata")
    int h = new Integer(5);

    int i = new Integer(99) + 1900;
    int j = 7 + 1900;

    //TODO:: error: (type.invalid)
    @Nullable int f;

    //TODO:: error: (type.invalid)
    @NonNull int g;
  }

  static void testDate() {
    @SuppressWarnings("deprecation") // for iCal4j
    int year = new java.util.Date().getYear() + 1900;
    String strDate = "/" + year;
  }

  //:: error: (type.invalid)
  @Nullable byte[] d1 = {4};
  byte @Nullable [] d1b = {4};

  @SuppressWarnings("ha!") byte[] d2 = {4};
}
