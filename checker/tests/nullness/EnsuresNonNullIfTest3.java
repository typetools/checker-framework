
import org.checkerframework.checker.nullness.qual.*;

public class EnsuresNonNullIfTest3 {

  void m(ValueTuple vt, int i) {

      if (!vt.isMissing(i)) {
        @NonNull Object val = vt.vals[i];
      }

  }

}

class ValueTuple {
  public @Nullable Object [] vals = new Object[10];

  @EnsuresNonNullIf(result=false, expression="vals[#1]")
  boolean isMissing(int value_index) {
    return true;
  }

}
