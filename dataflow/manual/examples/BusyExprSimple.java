class Test {
  public void test() {
    int a = 2, b = 3;
    if (a != b) {
      int x = b - a;
      int y = a - b;
    } else {
      int y = b - a;
      a = 0;
      int x = a - b;
    }
  }
}
