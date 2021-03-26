import java.util.ArrayList;
import java.util.List;

public final class ExtendsArrayList extends ArrayList<String> {

  public int removeMany(List<String> toRemove) {
    for (String inv : this) {
      if (!toRemove.contains(inv)) {}
    }
    return 0;
  }
}
