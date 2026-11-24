import java.util.List;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// The primary qualifier of the cast type equals the qualifier upper bound of `List`, but with
// -AcheckCastElementType the type arguments must still be checked.
public class CastToTopBoundType {
  @SuppressWarnings("unchecked")
  void untaintedElements(@Tainted Object taintedObject) {
    // :: warning: [cast.unsafe]
    List<@Untainted String> l = (List<@Untainted String>) taintedObject;
  }

  void safeUpcast(@Tainted List<@Untainted String> list) {
    List<@Untainted String> l = (@Tainted List<@Untainted String>) list;
  }
}
