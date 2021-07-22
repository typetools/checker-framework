import org.checkerframework.common.value.qual.IntVal;

import java.util.List;

public class DeclaredType<T extends @IntVal(1) Number> {
    List<T> field;
}
