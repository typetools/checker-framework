import java.util.List;
import java.util.Map;

public class Wildcards {
  List<?> l1;
  List<? extends Object> l2;
  Map<?, ?> l3;
  Map<? extends Map<?, String>, Object> l4;
}
