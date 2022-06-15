// A test that makes sure that inference doesn't crash when an array is
// used as a type parameter.

class ArrayAndTypevar {

  private class MyClass<T> {
    private T t;

    public MyClass(T t) {
      this.t = t;
    }
  }

  public void test() {
    MyClass<String[]> m = new MyClass<String[]>(new String[] {"foo", "bar"});
  }
}
