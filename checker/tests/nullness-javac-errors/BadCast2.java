class BadCast2 {
  public static void main(String[] args) {
    // :: error: illegal start of type
    String example = (@NonNull) "";
  }
}
