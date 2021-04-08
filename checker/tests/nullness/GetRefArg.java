import java.lang.reflect.*;
import org.checkerframework.checker.nullness.qual.*;

public class GetRefArg {
  private void get_ref_arg(Constructor<?> constructor) throws Exception {
    Object val = constructor.newInstance();
    // :: warning: (nulltest.redundant)
    assert val != null;
  }
}
