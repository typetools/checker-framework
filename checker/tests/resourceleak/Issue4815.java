// Test case for https://tinyurl.com/cfissue/4815
// @skip-test until the bug is fixed.

import java.util.List;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

public class Issue4815 {
  public <T extends Component> void initialize(
      List<T> list, @Owning @MustCall("initialize") T object) {
    object.initialize();
    list.add(object);
  }

  private static class Component {
    void initialize() {}
  }
}
