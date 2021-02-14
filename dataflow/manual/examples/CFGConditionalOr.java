class Test {
  void test(boolean b1, boolean b2, boolean b3) {
    int x = 0;
    if (b1 || (b2 || b3)) {
      x = 1;
    }
  }
}
