// Test case for https://tinyurl.com/cfissue/4815

import java.util.List;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

public class Issue4815 {
  public <T extends Component> void initialize(
      List<T> list, @Owning @MustCall("initialize") T object) {
    object.initialize();
    // `list` resolves to List<@MustCall Component> and thus cannot accept
    // an element of non-empty @MustCall type enforced by the Java
    // typechecker. This is an unfortunate consequence of the otherwise
    // elegant extension of the RLC to collections, which doesn't detect
    // that object already fulfilled its obligation here.
    // :: error: argument
    list.add(object);
  }

  private static class Component {
    void initialize() {}
  }
}
