import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.nullness.qual.*;

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
      @SuppressWarnings("nullness")
      @NonNull Foo f11 = null;
      fooField11 = f11;
    }
  }
}
