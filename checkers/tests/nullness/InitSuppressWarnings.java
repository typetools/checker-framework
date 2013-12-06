// @skip-test  "initialization" should work as a key to suppress this warning.

import checkers.nullness.quals.*;
import checkers.initialization.quals.*;

public class InitSuppressWarnings {

  private void init_vars (/*>>> @UnderInitialization(Object.class) @Raw InitSuppressWarnings this*/) {
    @SuppressWarnings({"rawness", "initialization"})
    /*@Initialized*/ /*@NonRaw*/ InitSuppressWarnings initializedThis = this;
  }

}
