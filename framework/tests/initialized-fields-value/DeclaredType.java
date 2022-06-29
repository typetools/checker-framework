import java.util.List;
import org.checkerframework.common.value.qual.IntVal;

public class DeclaredType<T extends @IntVal(1) Number> {
  List<T> field;
}
