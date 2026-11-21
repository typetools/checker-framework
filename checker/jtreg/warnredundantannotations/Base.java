import java.util.ArrayList;
import java.util.List;

public class Base {
  protected final List<Foo> foos = new ArrayList<>();

  public static Foo getFoo() {
    return new Foo();
  }
}
