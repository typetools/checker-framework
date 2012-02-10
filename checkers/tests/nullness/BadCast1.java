class BadCast1 {
  public void m() {
    //:: error: illegal start of expression :: error: not a statement
    (@NonNull) "";
  }
}
