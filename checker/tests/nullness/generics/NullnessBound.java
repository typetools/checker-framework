import org.checkerframework.checker.nullness.qual.*;

public class NullnessBound {

  public void test() {
    Gen1<@Nullable String> t1 = new Gen1<>();
    t1.add(null);

    Gen2<@Nullable String> t2 = new Gen2<>();
    t2.add(null);

    Gen1<@NonNull String> t3;
    // :: error: (type.argument)
    Gen2<@NonNull String> t4;
  }

  class Gen1<E extends @Nullable Object> {
    public void add(E e) {}
  }

  class Gen2<@Nullable E> {
    public void add(E e) {}
  }
}
