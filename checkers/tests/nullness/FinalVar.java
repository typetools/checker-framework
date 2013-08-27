import checkers.nullness.quals.NonNull;

class FinalVar {

  public Object pptIterator() {
    final String iter_view_1 = "I am not null";
    @NonNull String iter_view_2 = "Neither am I";
    final @NonNull String iter_view_3 = "Dittos";
    @NonNull String iter_view_4 = "I change but stay non-null";
    iter_view_4 = "new value";
    @NonNull String iter_view_5 = "I also change but stay non-null";
    iter_view_5 = "also new value";
    return new Object() {
      public void useFinalVar() {
        iter_view_1.hashCode();
        iter_view_2.hashCode();
        iter_view_3.hashCode();
        iter_view_4.hashCode();
        iter_view_5.hashCode();
      }
    };
  }

}
