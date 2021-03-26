import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.interning.qual.Interned;

public class InternMethodTest {

  private static Map<Foo, @Interned Foo> pool = new HashMap<>();

  class Foo {

    @SuppressWarnings("interning")
    public @Interned Foo intern() {
      if (!pool.containsKey(this)) {
        pool.put(this, (@Interned Foo) this);
      }
      return pool.get(this);
    }
  }

  void test() {
    Foo f = new Foo();
    @Interned Foo g = f.intern();
  }

  public static @Interned String intern(String a) {
    return (a == null) ? null : a.intern();
  }
}
