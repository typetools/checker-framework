import java.util.*;
import org.checkerframework.checker.nullness.qual.*;

public class Issue6849 {

  public static <T> T m(List<T> lst) {
    return lst.get(0);
  }

  public static void main(String[] args) {
    List<@Nullable Integer> lst = new LinkedList<>();
    lst.add(null);
    // :: error: (unboxing.of.nullable)
    int y = ((true) ? Issue6849.<@Nullable Integer>m(lst) : 10);
  }
}
