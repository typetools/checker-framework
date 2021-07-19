import java.util.List;
import org.checkerframework.checker.interning.qual.Interned;

public class NestedGenerics {

  public void test() {
    List<List<@Interned Object>> foo = bar();
  }

  public List<List<@Interned Object>> bar() {
    return null;
  }
}
