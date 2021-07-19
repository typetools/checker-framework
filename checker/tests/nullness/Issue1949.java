import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.*;

public class Issue1949 {
  public interface Base<R> {}

  public interface Child<R> extends Base<@Nullable R> {}

  public abstract static class BaseClass<R> implements Child<R> {
    abstract List<Child<R>> foo();
  }

  public static class ChildClass extends BaseClass<String> {

    @Override
    public List<Child<String>> foo() {
      return new ArrayList<>();
    }
  }
}
