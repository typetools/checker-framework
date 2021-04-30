import java.util.List;
import org.checkerframework.framework.testchecker.util.*;

public class WildcardSuper {
  void test(List<? super @Odd String> list) {
    // :: error: (assignment)
    @Odd Object odd = list.get(0);
  }
}
