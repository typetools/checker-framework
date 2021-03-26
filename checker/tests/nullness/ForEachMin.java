import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

class MyTop {
  List<String> children = new ArrayList<>();
}

abstract class PptRelationMin {
  abstract MyTop getPpt();

  void init_hierarchy_new() {
    MyTop ppt = getPpt();

    @NonNull Object o1 = ppt.children;

    for (String rel : ppt.children) {}

    @NonNull Object o2 = ppt.children;
  }
}
