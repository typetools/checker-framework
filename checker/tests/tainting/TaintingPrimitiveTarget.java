import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.tainting.qual.Untainted;

public class TaintingPrimitiveTarget {
  void method(List<Integer> list) {
    long l = Collections.min(list);
    // :: error: (assignment)
    // :: error: (type.arguments.not.inferred)
    @Untainted long l2 = Collections.min(list);
  }

  void method2(List<@Untainted Integer> list) {
    long l = Collections.min(list);
    @Untainted long l2 = Collections.min(list);
  }
}
