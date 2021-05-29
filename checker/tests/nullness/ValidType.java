import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class ValidType {

  void t1() {
    // :: error: (conflicting.annos)
    @NonNull @Nullable String l1;
    // :: error: (conflicting.annos)
    @UnderInitialization @UnknownInitialization String f;
  }
}
