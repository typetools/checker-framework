class AssignNullInExceptionBlock {

  static class Foo implements Comparable<Foo> {

    @Override
    public int compareTo(Foo other) {
      return 0;
    }
  }

  static Foo makeFoo() {
    throw new UnsupportedOperationException();
  }

  Foo fooField11;

  AssignNullInExceptionBlock() {
    try {
      fooField11 = makeFoo();
    } catch (Exception e) {
      Foo f11 = new Foo();
      fooField11 = f11;
    }
  }
}
