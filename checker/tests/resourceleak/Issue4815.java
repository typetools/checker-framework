// Test case for https://tinyurl.com/cfissue/4815

import java.util.List;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

public class Issue4815 {
  public <T extends Component> void initialize(
      // This error is a false positive, so if the checker stops finding it that would be fine.
      // :: error: (required.method.not.called)
      List<T> list, @Owning @MustCall("initialize") T object) {
    object.initialize();
    list.add(object);
  }

  private static class Component {
    void initialize() {}
  }
}
