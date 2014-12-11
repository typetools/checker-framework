// @skip-test  "initialization" should work as a key to suppress this warning.

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.initialization.qual.*;

public class InitSuppressWarnings {

  private void init_vars (/*>>> @UnderInitialization(Object.class) @Raw InitSuppressWarnings this*/) {
    @SuppressWarnings({"rawness", "initialization"})
    /*@Initialized*/ /*@NonRaw*/ InitSuppressWarnings initializedThis = this;
  }

}
