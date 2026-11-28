import java.util.List;
import org.checkerframework.checker.interning.qual.Interned;

// When an assignment boxes a value, the "found" type in an error message is the boxed type.
public class BoxingFoundType {
  void add(List<@Interned Integer> li, int i) {
    li.add(i);
  }
}
