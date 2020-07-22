class Test {
  void test(boolean b, int a) {
    int x = 1;
    int y = 0;
    if (b) {
      x = 2;
    } else {
      x = 2;
      y = a;
    }
    x = 3;
    if (a == 2) {
      x = 4;
    }
  }
}
