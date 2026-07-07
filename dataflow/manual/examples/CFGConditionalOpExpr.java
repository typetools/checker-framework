class Test {
  int test(boolean b) {
    int x = b ? this.hashCode() : 5;
    return x;
  }
}
