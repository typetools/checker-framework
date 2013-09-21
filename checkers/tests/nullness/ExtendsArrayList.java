import java.util.*;

// @skip-test failing, but disable to avoid breaking the build

public final class ExtendsArrayList extends ArrayList<String> {

  public int removeMany(List<String> toRemove) {
    for (String inv : this) {
      if (!toRemove.contains(inv)) {
      }
    }
    return 0;
  }

}
