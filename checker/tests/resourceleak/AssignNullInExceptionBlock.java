import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class AssignNullInExceptionBlock {

  static class Foo implements Comparable<Foo> {

    @Override
    public int compareTo(Foo other) {
      return 0;
    }
  }

  static Foo makeFoo() throws Exception {
    return new Foo();
  }

  Foo fooField;

  AssignNullInExceptionBlock() {
    try {
      fooField = makeFoo();
    } catch (Exception e) {
      @SuppressWarnings("nullness")
      @NonNull Foo f = null;
      fooField = f;
    }
  }
}
